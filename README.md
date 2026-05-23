# Multi-Channel Notification Engine (MCNE)

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.14-brightgreen?style=for-the-badge&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Message_Broker-ff6600?style=for-the-badge&logo=rabbitmq)
![Docker](https://img.shields.io/badge/Docker-Ready-blue?style=for-the-badge&logo=docker)

## Overview

The **Multi-Channel Notification Engine** is a backend service designed to centralize and standardize the delivery of notifications across multiple channels, such as Email, SMS, Webhooks, and Push Notifications.

By acting as a central hub in a microservices architecture, it allows other business domains to trigger notifications without needing to integrate directly with external providers or handle delivery failures.

## Architecture & Patterns

The architecture focuses on maintainability, extensibility, and high throughput:

- **Strategy Pattern**: The core engine uses the Strategy pattern to resolve channels. This allows new notification providers (e.g., Slack, WhatsApp) to be added without modifying existing core logic.
- **Asynchronous Processing**: Message brokers (RabbitMQ) and Java 21 Virtual Threads are used to handle I/O-bound tasks efficiently and prevent blocking operations.
- **Resiliency**: Built-in **Spring Retry** mechanism with exponential backoff to handle third-party API instability. If retries are exhausted, messages are securely routed to a **Dead Letter Queue (DLQ)** for later inspection and reprocessing, guaranteeing zero message loss.
- **Observability**: Spring Boot Actuator is integrated to provide health checks and metrics. Logging is strictly handled via SLF4J.

## Technology Stack

- **Language**: Java 21 (Virtual Threads, Records, Pattern Matching)
- **Framework**: Spring Boot 3.5.x
- **Database**: PostgreSQL (Relational persistence for notification logs and statuses)
- **Message Broker**: RabbitMQ (Asynchronous event-driven delivery)
- **Migrations**: Flyway (Database schema versioning)
- **Containerization**: Docker & Docker Compose

## Getting Started

### Prerequisites

- Java 21 SDK
- Maven 3.8+
- Docker & Docker Compose

### Running the application locally

1. **Clone the repository:**

   ```bash
   git clone https://github.com/your-username/multi-channel-notification-engine.git
   cd multi-channel-notification-engine
   ```

2. **Run the infrastructure (Database & Broker):**

   ```bash
   docker compose up -d
   ```

   _Tip: You can access the RabbitMQ Management UI at `http://localhost:15672` (username: `guest`, password: `guest`)._

3. **Start the application:**

   ```bash
   ./mvnw spring-boot:run
   ```

4. **Verify Health Status:**
   The application exposes a custom health check endpoint:

   ```bash
   curl -X GET http://localhost:8081/api/v1/status
   ```

   Expected Response:

   ```json
   {
     "status": "Up and Running",
     "environment": "Development",
     "timestamp": "2026-05-10T15:00:00Z"
   }
   ```

5. **Send a Notification:**
   Use the following `cURL` command to dispatch an asynchronous notification request:

   ```bash
   curl -i -X POST http://localhost:8081/api/v1/notifications \
   -H "Content-Type: application/json" \
   -d '{
     "recipient": "user@example.com",
     "message": "Welcome to our platform!",
     "channel": "EMAIL",
     "metadata": {}
   }'
   ```

   _Note: The API will return `202 Accepted` immediately, and the actual processing will occur asynchronously._

6. **Reprocess Dead Letter Queue (DLQ):**
   If a third-party provider is down, messages will fail after 3 retries and be moved to the DLQ (`notification.dlq`). Once the provider is back online, you can reprocess them using:
   ```bash
   curl -i -X POST http://localhost:8081/api/v1/notifications/dlq/reprocess
   ```
   Expected Response:
   ```json
   {
     "message": "10 messages reprocessed successfully."
   }
   ```
   ```

   ```
