# System Architecture - Multi-Channel Notification Engine (MCNE)

This document provides a visual and technical breakdown of the MCNE architecture, focusing on its decoupled, asynchronous, extensible, and secure design.

## 1. High-Level System Architecture

The system is designed around an event-driven architecture using RabbitMQ to decouple the HTTP ingestion layer from the actual external API delivery layer. This ensures that a spike in incoming requests does not overload external providers and prevents clients from waiting for synchronous I/O operations.

All API endpoints are protected by an API key filter (`SecurityConfig`). The WebSocket endpoint and Actuator are intentionally public.

```mermaid
graph TD
    Client([Client / Microservice]) -->|HTTP POST + X-API-Key| SecurityFilter[API Key Filter]
    SecurityFilter -->|Authenticated| Controller[Notification Controller]
    SecurityFilter -->|Invalid Key| Unauthorized[401 Unauthorized]

    subgraph MCNE Core Application
        Controller -->|Validates & Dispatches| Dispatcher[Notification Dispatcher Service]
        Dispatcher -->|Saves PENDING state| DB[(PostgreSQL)]
        Dispatcher -->|Publishes| Producer[Notification Producer]
        Dispatcher -->|WS: RECEIVED, QUEUED| WSPublisher[WebSocket Event Publisher]
    end

    Producer -->|AMQP| RabbitMQ{RabbitMQ Broker}

    RabbitMQ -->|Consumes| Consumer[Notification Consumer]

    subgraph Async Worker
        Consumer -->|WS: PROCESSING| WSPublisher
        Consumer -->|Processes| AsyncDispatcher[Notification Dispatcher Service]
        AsyncDispatcher -->|Resolves| Strategies{Notification Strategies}
        Strategies -->|EmailStrategy| EmailAPI[AWS SES]
        Strategies -->|SMSStrategy| SMSAPI[AWS SNS]
        Strategies -->|WS: SENT / RETRYING / DLQ| WSPublisher
        Strategies -->|Updates state SENT/FAILED| DB
    end

    WSPublisher -->|STOMP /topic/notifications| Frontend([Frontend Visualizer])

    RabbitMQ -->|On exhausted retries| DLQ[(Dead Letter Queue)]
    DLQ -->|POST /dlq/reprocess| Controller

    classDef client fill:#e2e3e5,stroke:#6c757d,stroke-width:2px,color:#000000;
    classDef security fill:#f8d7da,stroke:#dc3545,stroke-width:2px,color:#000000;
    classDef core fill:#cce5ff,stroke:#007bff,stroke-width:2px,color:#000000;
    classDef infra fill:#f8d7da,stroke:#dc3545,stroke-width:2px,color:#000000;
    classDef worker fill:#fff3cd,stroke:#ffc107,stroke-width:2px,color:#000000;
    classDef external fill:#d4edda,stroke:#28a745,stroke-width:2px,color:#000000;

    class Client,Frontend client;
    class SecurityFilter,Unauthorized security;
    class Controller,Dispatcher,Producer,WSPublisher core;
    class DB,RabbitMQ,DLQ infra;
    class Consumer,AsyncDispatcher,Strategies worker;
    class EmailAPI,SMSAPI external;
```

## 2. Sequence Diagram (Asynchronous Flow, Retry and DLQ)

This diagram illustrates the full chronological flow of a notification request, including authentication, the WebSocket event lifecycle, retry mechanism, and Dead Letter Queue routing on persistent failure.

```mermaid
sequenceDiagram
    autonumber
    actor Client

    box #f8d7da Security
        participant Auth as API Key Filter
    end
    box #85C1E9 MCNE Core
        participant API as REST Controller
        participant WS as WebSocket Publisher
    end
    box #F5B041 Infrastructure
        participant DB as PostgreSQL (Log)
        participant MQ as RabbitMQ
        participant DLQ as Dead Letter Queue
    end
    box #F7DC6F Async Worker
        participant Worker as Async Consumer
        participant Strategy as Strategy (Email/SMS/...)
    end
    box #82E0AA External Services
        participant External as Provider (AWS SES, SNS, Twilio...)
    end

    Client->>Auth: POST /api/v1/notifications (X-API-Key header)
    alt Invalid or missing key
        Auth-->>Client: 401 Unauthorized
    else Valid key
        Auth->>API: Forward request
    end

    activate API
    API->>DB: INSERT NotificationLog (Status: PENDING)
    DB-->>API: Log ID generated
    API->>WS: Emit RECEIVED event
    API->>MQ: Publish NotificationEvent
    API->>WS: Emit QUEUED event
    API-->>Client: HTTP 202 Accepted
    deactivate API

    Note over Client,API: The client is now free. The rest happens asynchronously.

    MQ->>Worker: Consume Message
    activate Worker
    Worker->>WS: Emit PROCESSING event
    Worker->>Strategy: strategy.send(logId, request)
    activate Strategy
    Strategy->>External: HTTP Request to provider API

    alt Successful Delivery
        External-->>Strategy: HTTP 200 OK
        Strategy->>WS: Emit SENT event
        Strategy->>DB: UPDATE NotificationLog (Status: SENT)
    else Transient Failure (network error)
        External-->>Strategy: Timeout / Connection error
        Strategy->>WS: Emit RETRYING event
        Note over Strategy: @Retryable kicks in, up to 3 attempts with exponential backoff (2s, 4s)
        Strategy->>External: Retry attempt...
        External-->>Strategy: HTTP 200 OK
        Strategy->>WS: Emit SENT event
        Strategy->>DB: UPDATE NotificationLog (Status: SENT)
    else Persistent Failure (all retries exhausted)
        External-->>Strategy: Permanent error
        Strategy->>DB: UPDATE NotificationLog (Status: FAILED)
        Strategy->>WS: Emit DLQ event
        Worker->>MQ: AmqpRejectAndDontRequeueException
        MQ->>DLQ: Route message to Dead Letter Queue
    end

    deactivate Strategy
    deactivate Worker

    Note over DLQ: Message waits in DLQ until manually reprocessed.
    Client->>Auth: POST /api/v1/notifications/dlq/reprocess (X-API-Key)
    Auth->>API: Forward request
    API->>DLQ: Pull all messages
    API->>MQ: Re-publish to main exchange
```

## 3. Extensibility: The Strategy Pattern

The engine uses the **Strategy Design Pattern** to adhere to the Open/Closed Principle (OCP) from SOLID. The `NotificationDispatcherService` resolves the correct strategy at runtime by calling `supports(channel)` on each registered bean, with no knowledge of which providers exist.

Adding a new delivery channel requires only one new class:

```java
@Component
public class PushNotificationStrategy implements NotificationStrategy {
    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.PUSH;
    }

    @Override
    public void send(UUID logId, NotificationRequest request) {
        // Firebase FCM, APNs, or any push provider
    }
}
```

Spring automatically picks it up and injects it into the strategy list. The same pattern applies to any other channel: Twilio SMS, Slack, Microsoft Teams, outbound Webhooks, WhatsApp Business API, and so on.

```mermaid
classDiagram
    class NotificationDispatcherService {
        - List~NotificationStrategy~ strategies
        - boolean demoMode
        + dispatchToQueue(NotificationRequest)
        + processFromQueue(NotificationEvent)
        - applyDemoDelay(NotificationRequest)
    }

    class NotificationStrategy {
        <<interface>>
        + supports(NotificationChannel) boolean
        + send(UUID logId, NotificationRequest) void
    }

    class EmailNotificationStrategy {
        - SesClient sesClient
        - boolean demoMode
        + supports(NotificationChannel) boolean
        + send(UUID logId, NotificationRequest) void
    }

    class SmsNotificationStrategy {
        - SnsClient snsClient
        - boolean demoMode
        + supports(NotificationChannel) boolean
        + send(UUID logId, NotificationRequest) void
    }

    class PushNotificationStrategy {
        + supports(NotificationChannel) boolean
        + send(UUID logId, NotificationRequest) void
    }

    class WebhookNotificationStrategy {
        + supports(NotificationChannel) boolean
        + send(UUID logId, NotificationRequest) void
    }

    NotificationDispatcherService --> NotificationStrategy : Delegates sending
    NotificationStrategy <|.. EmailNotificationStrategy : Implements
    NotificationStrategy <|.. SmsNotificationStrategy : Implements
    NotificationStrategy <|.. PushNotificationStrategy : Planned
    NotificationStrategy <|.. WebhookNotificationStrategy : Planned

    style NotificationDispatcherService fill:#cce5ff,stroke:#007bff,stroke-width:2px,color:#000000
    style NotificationStrategy fill:#d4edda,stroke:#28a745,stroke-width:2px,color:#000000
    style EmailNotificationStrategy fill:#fff3cd,stroke:#ffc107,stroke-width:2px,color:#000000
    style SmsNotificationStrategy fill:#fff3cd,stroke:#ffc107,stroke-width:2px,color:#000000
    style PushNotificationStrategy fill:#e8e8e8,stroke:#aaaaaa,stroke-width:2px,color:#000000
    style WebhookNotificationStrategy fill:#e8e8e8,stroke:#aaaaaa,stroke-width:2px,color:#000000
```

## 4. Security Architecture

### API Key Authentication

All `POST` and `PUT` endpoints under `/api/**` are protected by a stateless API key filter implemented in `SecurityConfig`.

Flow:
1. Client sends `X-API-Key: <key>` with every request.
2. `apiKeyFilter` (a `OncePerRequestFilter`) compares the header value against the configured key.
3. On match: populates the `SecurityContext` with a `UsernamePasswordAuthenticationToken` (role `ROLE_API`) and forwards the request.
4. On mismatch: returns `401 Unauthorized` with a JSON error body and never reaches the controller.

Public endpoints (no key required):
- `GET /api/v1/status`
- `GET /actuator/health`, `GET /actuator/info`
- WebSocket handshake at `/ws-mcne/**`

| Property | Env Variable | Default (dev) |
|---|---|---|
| `mcne.security.api-key` | `MCNE_API_KEY` | `dev-only-key` |

Change `MCNE_API_KEY` to a strong random value before exposing the service publicly.

### CORS

CORS is managed centrally in `CorsConfig`. Allowed origins are driven by the `MCNE_ALLOWED_ORIGINS` environment variable (comma-separated). There are no `@CrossOrigin` annotations on individual controllers.

| Property | Env Variable | Default (dev) |
|---|---|---|
| `mcne.cors.allowed-origins` | `MCNE_ALLOWED_ORIGINS` | `http://localhost:5173` |

### WebSocket Privacy

WebSocket events broadcast to `/topic/notifications` carry only `logId`, `eventType`, and `channel`. The recipient address and message body are never included in broadcast payloads.

### AWS Credentials

`AwsConfig` resolves credentials in the following order:

1. `DefaultCredentialsProvider` (IAM role, instance profile, or environment variables), used when `AWS_ACCESS_KEY` is blank. This is the expected path in staging and production.
2. Static fallback via `StaticCredentialsProvider`, used only when explicit keys are set through `AWS_ACCESS_KEY` and `AWS_SECRET_KEY`. Intended for local development only.

## 5. Database Schema

The `notification_log` table is the single source of truth for tracking the lifecycle of every dispatched notification.

```mermaid
erDiagram
    NOTIFICATION_LOG {
        UUID id PK "Generated (UUID strategy)"
        VARCHAR customer_name_email "Recipient address or phone number (max 100)"
        VARCHAR message "Notification body text (max 300)"
        VARCHAR channel "EMAIL | SMS | PUSH | ..."
        VARCHAR status "PENDING | SENT | FAILED"
        TIMESTAMP created_at "Auto-set on insert"
    }
```

Status lifecycle:

```
PENDING  ->  SENT    (delivery confirmed by external provider)
PENDING  ->  FAILED  (all retry attempts exhausted)
FAILED   ->  PENDING (via DLQ reprocessing endpoint)
```

## 6. Retry and DLQ Architecture

The resiliency pipeline consists of two independent layers:

| Layer | Mechanism | Scope | Configuration |
|---|---|---|---|
| Application-level | `@Retryable(SdkClientException)` | Transient network errors | 3 attempts, 2s/4s backoff |
| Broker-level | RabbitMQ Dead Letter Exchange (DLX) | Messages rejected after all retries | Durable DLQ, manual reprocessing |

`@Retryable` is configured to only retry `SdkClientException` (network and timeout errors). Permanent provider errors (e.g. unverified SES sender address) are not retried and go directly to the DLQ.

## 7. Real-Time Observability (WebSockets)

The engine implements a non-blocking Observer Pattern via STOMP WebSockets (`/ws-mcne`). The `WebSocketEventPublisher` service broadcasts state changes to `/topic/notifications`. Each event payload contains `logId`, `eventType` (a `NotificationEventType` enum), and `channel`, with no PII transmitted.

| Event | Emitted by | Meaning |
|---|---|---|
| `RECEIVED` | `NotificationDispatcherService` | HTTP request accepted, log entry created |
| `QUEUED` | `NotificationDispatcherService` | Message published to RabbitMQ |
| `PROCESSING` | `NotificationConsumer` | Worker thread picked up the message |
| `RETRYING` | Strategy implementations | Transient error caught, Spring Retry will attempt again |
| `SENT` | Strategy implementations | Delivery confirmed by the external provider |
| `DLQ` | `NotificationDispatcherService` | All retries exhausted, message routed to Dead Letter Queue |

### Demo Mode

A `demo` Spring profile activates additional behaviour for portfolio demonstration:

- `DemoConfig` registers a servlet filter that marks requests containing `X-MCNE-Client: Visualizer`.
- `NotificationConsumer` and strategy implementations apply artificial delays (`demoDelayMs` metadata) to make the pipeline flow visible in the Visualizer frontend.
- Strategies accept `simulateError=true` to force `SdkClientException`, triggering the retry and DLQ flow without real AWS calls.

The entire demo code path is inactive when the `demo` profile is not enabled. The `demoMode` flag is injected via `@Value("#{environment.acceptsProfiles('demo')}")` and short-circuits all simulation logic at runtime.

To activate: `--spring.profiles.active=demo`

## 8. Configuration Reference

### Environment Variables

| Variable | Property | Description | Default (dev) |
|---|---|---|---|
| `DB_USERNAME` | `spring.datasource.username` | PostgreSQL username | `mcne_user` |
| `DB_PASSWORD` | `spring.datasource.password` | PostgreSQL password | `mcne_password` |
| `DB_NAME` | `spring.datasource.url` | PostgreSQL database name | `mcne_db` |
| `AWS_REGION` | `aws.region` | AWS region for SES/SNS | `us-east-2` |
| `AWS_ACCESS_KEY` | `aws.accessKeyId` | AWS access key (blank = use DefaultCredentialsProvider) | _(blank)_ |
| `AWS_SECRET_KEY` | `aws.secretKey` | AWS secret key (blank = use DefaultCredentialsProvider) | _(blank)_ |
| `AWS_VERIFIED_EMAIL` | `aws.ses.verified-email` | Verified SES sender email | `none@example.com` |
| `MCNE_API_KEY` | `mcne.security.api-key` | API key for `X-API-Key` header | `dev-only-key` |
| `MCNE_ALLOWED_ORIGINS` | `mcne.cors.allowed-origins` | Comma-separated allowed CORS origins | `http://localhost:5173` |

### Infrastructure Ports (Local Docker)

| Service | Internal Port | External Port |
|---|---|---|
| PostgreSQL | 5432 | 5435 (non-standard to avoid conflicts) |
| RabbitMQ AMQP | 5672 | 5672 |
| RabbitMQ UI | 15672 | 15672 |
| MCNE App | 8081 | 8081 |
