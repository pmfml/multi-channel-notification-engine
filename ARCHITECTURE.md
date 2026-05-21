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

    classDef client fill:#d4edda,stroke:#28a745,stroke-width:2px,color:#000000;
    classDef core fill:#cce5ff,stroke:#007bff,stroke-width:2px,color:#000000;
    classDef infra fill:#f8d7da,stroke:#dc3545,stroke-width:2px,color:#000000;
    classDef worker fill:#fff3cd,stroke:#ffc107,stroke-width:2px,color:#000000;
    classDef external fill:#e2e3e5,stroke:#6c757d,stroke-width:2px,color:#000000;

    class Client client;
    class Controller,Dispatcher,Producer core;
    class DB,RabbitMQ infra;
    class Consumer,AsyncDispatcher,Strategies worker;
    class EmailAPI,SMSAPI external;
```

---

## 2. Sequence Diagram (Asynchronous Flow & Timing)

This diagram illustrates the chronological flow of a notification request. Notice how the HTTP thread is freed almost immediately (returning a `202 Accepted`), while the heavy lifting is done in the background by a separate worker thread.

```mermaid
sequenceDiagram
    autonumber
    actor Client

    box rgb(204, 229, 255) MCNE Core
        participant API as REST Controller
    end
    box rgb(248, 215, 218) Infrastructure
        participant DB as PostgreSQL (Log)
        participant MQ as RabbitMQ
    end
    box rgb(255, 243, 205) Async Worker
        participant Worker as Async Consumer
        participant Strategy as Strategy (Email/SMS)
    end
    box rgb(226, 227, 229) External Services
        participant External as External Provider
    end

    Client->>API: POST /api/v1/notifications
    activate API
    API->>DB: INSERT NotificationLog (Status: PENDING)
    DB-->>API: Log ID generated
    API->>MQ: Publish NotificationRequest Message
    API-->>Client: HTTP 202 Accepted
    deactivate API

    Note over Client,API: The client is now free. The rest happens asynchronously.

    MQ->>Worker: Consume Message
    activate Worker
    Worker->>Strategy: strategy.send(request)
    activate Strategy
    Strategy->>External: HTTP Request to 3rd Party API

    alt Success Delivery
        External-->>Strategy: HTTP 200 OKz
        Strategy->>DB: UPDATE NotificationLog (Status: SENT)
    else Failed Delivery
        External-->>Strategy: HTTP 500 / Timeout
        Strategy->>DB: UPDATE NotificationLog (Status: FAILED)
        Note over Strategy,Worker: (Future) Retry mechanism kicks in here
    end

    deactivate Strategy
    deactivate Worker
```

---

## 3. Component Diagram (The Strategy Pattern)

The engine uses the **Strategy Design Pattern** to adhere to the Open/Closed Principle (OCP) from SOLID. If we need to add a new channel (e.g., Push Notification or Slack), we simply create a new class implementing the `NotificationStrategy` interface without touching the core `NotificationDispatcherService`.

```mermaid
classDiagram
    class NotificationDispatcherService {
        - List~NotificationStrategy~ strategies
        + dispatchToQueue(NotificationRequest)
        + processFromQueue(NotificationRequest)
    }

    class NotificationStrategy {
        <<interface>>
        + supports(NotificationChannel) boolean
        + send(NotificationRequest) void
    }

    class EmailNotificationStrategy {
        + supports(NotificationChannel) boolean
        + send(NotificationRequest) void
    }

    class SmsNotificationStrategy {
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
