# System Architecture - Multi-Channel Notification Engine (MCNE)

This document provides a visual and technical breakdown of the MCNE architecture, focusing on its decoupled, asynchronous, and extensible design.

---

## 1. High-Level System Architecture

The system is designed around an event-driven architecture using RabbitMQ to decouple the HTTP ingestion layer from the actual external API delivery layer. This ensures that a spike in incoming requests does not overload external providers (like AWS SES or Twilio) and prevents clients from waiting for synchronous I/O operations.

```mermaid
graph TD
    Client([Client / Microservice]) -->|HTTP POST| Controller[Notification Controller]

    subgraph MCNE Core Application
        Controller -->|Validates & Dispatches| Dispatcher[Notification Dispatcher Service]
        Dispatcher -->|Saves PENDING state| DB[(PostgreSQL)]
        Dispatcher -->|Publishes| Producer[Notification Producer]
    end

    Producer -->|AMQP| RabbitMQ{RabbitMQ Broker}

    RabbitMQ -->|Consumes| Consumer[Notification Consumer]

    subgraph Async Worker
        Consumer -->|Processes| AsyncDispatcher[Notification Dispatcher Service]
        AsyncDispatcher -->|Resolves| Strategies{Notification Strategies}
        Strategies -->|EmailStrategy| EmailAPI[AWS SES / SendGrid]
        Strategies -->|SMSStrategy| SMSAPI[Twilio / AWS SNS]
        Strategies -->|Updates state SENT/FAILED| DB
    end

    RabbitMQ -->|On exhausted retries| DLQ[(Dead Letter Queue)]
    DLQ -->|POST /dlq/reprocess| Controller

    %% Styling
    classDef client fill:#e2e3e5,stroke:#6c757d,stroke-width:2px,color:#000000;
    classDef core fill:#cce5ff,stroke:#007bff,stroke-width:2px,color:#000000;
    classDef infra fill:#f8d7da,stroke:#dc3545,stroke-width:2px,color:#000000;
    classDef worker fill:#fff3cd,stroke:#ffc107,stroke-width:2px,color:#000000;
    classDef external fill:#d4edda,stroke:#28a745,stroke-width:2px,color:#000000;

    class Client client;
    class Controller,Dispatcher,Producer core;
    class DB,RabbitMQ,DLQ infra;
    class Consumer,AsyncDispatcher,Strategies worker;
    class EmailAPI,SMSAPI external;
```

---

## 2. Sequence Diagram (Asynchronous Flow, Retry & DLQ)

This diagram illustrates the full chronological flow of a notification request, including the retry mechanism and Dead Letter Queue routing on persistent failure.

```mermaid
sequenceDiagram
    autonumber
    actor Client

    box #85C1E9 MCNE Core
        participant API as REST Controller
    end
    box #F5B041 Infrastructure
        participant DB as PostgreSQL (Log)
        participant MQ as RabbitMQ
        participant DLQ as Dead Letter Queue
    end
    box #F7DC6F Async Worker
        participant Worker as Async Consumer
        participant Strategy as Strategy (Email/SMS)
    end
    box #82E0AA External Services
        participant External as External Provider
    end

    Client->>API: POST /api/v1/notifications
    activate API
    API->>DB: INSERT NotificationLog (Status: PENDING)
    DB-->>API: Log ID generated
    API->>MQ: Publish NotificationEvent
    API-->>Client: HTTP 202 Accepted
    deactivate API

    Note over Client,API: The client is now free. The rest happens asynchronously.

    MQ->>Worker: Consume Message
    activate Worker
    Worker->>Strategy: strategy.send(request)
    activate Strategy
    Strategy->>External: HTTP Request to 3rd Party API

    alt Successful Delivery
        External-->>Strategy: HTTP 200 OK
        Strategy->>DB: UPDATE NotificationLog (Status: SENT)
    else Transient Failure (network error)
        External-->>Strategy: Timeout / Connection error
        Note over Strategy: @Retryable kicks in — up to 3 attempts<br/>with exponential backoff (2s, 4s)
        Strategy->>External: Retry attempt...
        External-->>Strategy: HTTP 200 OK
        Strategy->>DB: UPDATE NotificationLog (Status: SENT)
    else Persistent Failure (all retries exhausted)
        External-->>Strategy: Permanent error
        Strategy->>DB: UPDATE NotificationLog (Status: FAILED)
        Worker->>MQ: AmqpRejectAndDontRequeueException
        MQ->>DLQ: Route message to Dead Letter Queue
    end

    deactivate Strategy
    deactivate Worker

    Note over DLQ: Message waits in DLQ until manually reprocessed.
    Client->>API: POST /api/v1/notifications/dlq/reprocess
    API->>DLQ: Pull all messages
    API->>MQ: Re-publish to main exchange
```

---

## 3. Component Diagram (The Strategy Pattern)

The engine uses the **Strategy Design Pattern** to adhere to the Open/Closed Principle (OCP) from SOLID. If we need to add a new channel (e.g., Push Notification or Slack), we simply create a new class implementing the `NotificationStrategy` interface without touching the core `NotificationDispatcherService`.

```mermaid
classDiagram
    class NotificationDispatcherService {
        - List~NotificationStrategy~ strategies
        + dispatchToQueue(NotificationRequest)
        + processFromQueue(NotificationEvent)
    }

    class NotificationStrategy {
        <<interface>>
        + supports(NotificationChannel) boolean
        + send(NotificationRequest) void
    }

    class EmailNotificationStrategy {
        - SesClient sesClient
        + supports(NotificationChannel) boolean
        + send(NotificationRequest) void
    }

    class SmsNotificationStrategy {
        - SnsClient snsClient
        + supports(NotificationChannel) boolean
        + send(NotificationRequest) void
    }

    NotificationDispatcherService --> NotificationStrategy : Delegates sending
    NotificationStrategy <|.. EmailNotificationStrategy : Implements
    NotificationStrategy <|.. SmsNotificationStrategy : Implements

    style NotificationDispatcherService fill:#cce5ff,stroke:#007bff,stroke-width:2px,color:#000000
    style NotificationStrategy fill:#d4edda,stroke:#28a745,stroke-width:2px,color:#000000
    style EmailNotificationStrategy fill:#fff3cd,stroke:#ffc107,stroke-width:2px,color:#000000
    style SmsNotificationStrategy fill:#fff3cd,stroke:#ffc107,stroke-width:2px,color:#000000
```

---

## 4. Database Schema

The `notification_log` table is the single source of truth for tracking the lifecycle of every dispatched notification.

```mermaid
erDiagram
    NOTIFICATION_LOG {
        UUID id PK "Generated (UUID strategy)"
        VARCHAR customer_name_email "Recipient address or phone number (max 100)"
        VARCHAR message "Notification body text (max 300)"
        VARCHAR channel "EMAIL | SMS | PUSH"
        VARCHAR status "PENDING | SENT | FAILED"
        TIMESTAMP created_at "Auto-set on insert"
    }
```

**Status Lifecycle:**

```
PENDING  →  SENT    (delivery confirmed by external provider)
PENDING  →  FAILED  (all retry attempts exhausted)
FAILED   →  PENDING (via DLQ reprocessing endpoint)
```

---

## 5. Retry & DLQ Architecture

The resiliency pipeline consists of two independent layers:

| Layer | Mechanism | Scope | Configuration |
|---|---|---|---|
| **Application-level** | `@Retryable(SdkClientException)` | Transient network errors | 3 attempts, 2s/4s backoff |
| **Broker-level** | RabbitMQ Dead Letter Exchange (DLX) | Messages rejected after all retries | Durable DLQ, manual reprocessing |

> **Important:** `@Retryable` is configured to only retry `SdkClientException` (network/timeout errors). Permanent AWS service errors (e.g., unverified sender address) are **not** retried and go directly to the DLQ.

---

## 6. Real-Time Observability (WebSockets)

To allow for real-time visual tracking of the asynchronous lifecycle, the engine implements a non-blocking **Observer Pattern** via STOMP WebSockets (`/ws-mcne`).

The `WebSocketEventPublisher` service broadcasts state changes to the `/topic/notifications` channel. These events include:
*   `QUEUED`: Emitted by the `NotificationDispatcherService` when a message is successfully sent to RabbitMQ.
*   `PROCESSING`: Emitted by the `NotificationConsumer` when a worker thread picks up the message.
*   `SENT`: Emitted by the strategies (`EmailNotificationStrategy`, `SmsNotificationStrategy`) upon successful AWS delivery.
*   `RETRYING`: Emitted by strategies when catching a transient `SdkClientException` before Spring Retry kicks in.
*   `DLQ`: Emitted before routing a permanently failed message to the Dead Letter Exchange.

**Demo & Simulation Mode:**
For demonstration and portfolio purposes, the engine supports a secure simulation mode. If an incoming HTTP request contains the header `X-MCNE-Client: Visualizer`, the engine allows the injection of specific metadata flags (`demoDelayMs` for artificial queue bottlenecking and `simulateError=true` to force AWS SDK exceptions). This allows external visualizers to demonstrate the retry and DLQ resiliency mechanisms safely, without polluting production traffic.
