# Multi-Channel Notification Engine (MCNE) ✉️📱🔔

**Multi-Channel Notification Engine (MCNE)** is a high-throughput backend service designed to centralize and standardize the delivery of notifications across multiple communication channels (Email, SMS, Push Notifications, Webhooks, and more).

Acting as a central gateway in a microservices architecture, it allows different business domains to trigger outbound communication asynchronously without integrating directly with third-party providers or dealing with network retry logic.

## 🚀 Key Features

- **Strategy-Based Routing:** Uses the Strategy design pattern to dynamically route notification payloads to their respective channel providers. Currently ships with Email (AWS SES) and SMS (AWS SNS) implementations. Adding a new channel (Twilio, Firebase Push, Slack, Webhook, or any other provider) requires only a new class implementing `NotificationStrategy`, with zero changes to the core engine.
- **Asynchronous Core:** Leverages Spring AMQP (RabbitMQ) queues and Java 21 Virtual Threads to achieve high concurrent I/O throughput.
- **Resiliency and Fault Tolerance:** Integrated with Spring Retry offering configurable exponential backoff (retries only on transient network errors). Failed deliveries are safely offloaded to a Dead Letter Queue (DLQ).
- **Real-Time Observability:** Broadcasts asynchronous state changes (`RECEIVED`, `QUEUED`, `PROCESSING`, `RETRYING`, `SENT`, `DLQ`) via STOMP WebSockets, allowing seamless integration with frontend visualizers.
- **API Key Authentication:** All API endpoints (`/api/**`) are protected by a static API key sent via the `X-API-Key` header. Public endpoints (`/api/v1/status`, `/actuator/**`, `/ws-mcne/**`) are intentionally open.
- **DLQ Recovery API:** Endpoint to trigger bulk reprocessing of dead-lettered notifications once external providers recover.
- **Demo Mode:** An isolated `demo` Spring profile enables the Visualizer frontend to inject artificial delays and simulated errors. This code path is completely inactive in production.
- **Observability:** Health and system diagnostics exposed via Spring Boot Actuator (`/actuator/health`, `/actuator/info`).

## 🛠️ Technology Stack

- **Language:** Java 21 (Virtual Threads, Records, Pattern Matching)
- **Framework:** Spring Boot 3.5.x with Spring Data JPA
- **Security:** Spring Security (API key authentication via `X-API-Key` header)
- **Message Broker:** RabbitMQ
- **Database:** PostgreSQL 16 (schema managed by `spring.jpa.hibernate.ddl-auto`)
- **AWS Providers:** SES (Email), SNS (SMS) via AWS SDK v2
- **Local Containerization:** Docker / Docker Compose
- **Testing:** JUnit 5, Mockito, Spring WebMvcTest, H2 (in-memory for repository tests)

## 📋 Prerequisites

To run this application locally, you will need:

1. Java JDK 21 or higher.
2. Docker and Docker Compose installed and running.
3. Maven 3.8+ (or use the included `./mvnw` wrapper).

## ⚙️ How to Get Started

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/multi-channel-notification-engine.git
cd multi-channel-notification-engine
```

### 2. Configure Environment Variables

The application reads sensitive configuration from environment variables. Copy the template and fill in your values:

```bash
cp .env.example .env
```

Edit `.env` with your actual credentials:

```bash
# PostgreSQL (used by Docker Compose)
DB_USERNAME=mcne_user
DB_PASSWORD=your-secure-password
DB_NAME=mcne_db

# AWS - leave blank to use IAM role / instance profile (recommended for production)
# Set real values only for local development
AWS_REGION=us-east-2
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_VERIFIED_EMAIL=no-reply@yourdomain.com

# API key protecting all /api/** endpoints (X-API-Key header)
MCNE_API_KEY=your-strong-random-key

# CORS - comma-separated list of allowed frontend origins
MCNE_ALLOWED_ORIGINS=http://localhost:5173
```

> The `.env` file is listed in `.gitignore` and must **never** be committed. Use `.env.example` as the versioned template.

### 3. Infrastructure Setup (Docker)

The project includes a `docker-compose.yml` file defining the PostgreSQL database (port **5435** on localhost) and the RabbitMQ broker (management console on port **15672**).

```bash
docker compose up -d
```

Tip: access the RabbitMQ Management UI at `http://localhost:15672` (default credentials: `guest` / `guest`).

### 4. Build and Run Tests

Execute the unit and integration testing suite (no Docker required for unit/slice tests):

```bash
./mvnw clean test
```

### 5. Running the Application

Standard mode:

```bash
./mvnw spring-boot:run
```

Demo mode (enables Visualizer delays and simulated errors; never use in production):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

The REST API will be available at `http://localhost:8081`.

## 🔐 Authentication

All `POST` and `PUT` endpoints require an API key sent in the request header:

```
X-API-Key: your-api-key
```

The key is configured via the `MCNE_API_KEY` environment variable (default for local dev: `dev-only-key`).

Endpoints that do **not** require authentication:

- `GET /api/v1/status`
- `GET /actuator/health`
- `GET /actuator/info`
- WebSocket handshake at `/ws-mcne`

## 📡 REST API Documentation

| Method | Endpoint | Auth | Description | Status Code |
| :--- | :--- | :---: | :--- | :--- |
| GET | `/api/v1/status` | No | Application health and uptime | `200 OK` |
| GET | `/actuator/health` | No | Spring Boot health check | `200 OK` |
| POST | `/api/v1/notifications` | Yes | Submits an asynchronous notification dispatch request | `202 Accepted` |
| POST | `/api/v1/notifications/dlq/reprocess` | Yes | Retries all failed messages stored in the DLQ | `200 OK` |
| PUT | `/api/v1/config/concurrency?count={n}` | Yes | Sets the number of concurrent RabbitMQ consumers (0 stops the consumer) | `200 OK` |

### Sample Requests

Dispatch an Email notification:

```bash
curl -X POST http://localhost:8081/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-only-key" \
  -d '{
    "recipient": "user@example.com",
    "message": "Welcome to our platform!",
    "channel": "EMAIL",
    "metadata": {}
  }'
```

Dispatch an SMS notification:

```bash
curl -X POST http://localhost:8081/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-only-key" \
  -d '{
    "recipient": "+5511999999999",
    "message": "Your verification code is 1234.",
    "channel": "SMS",
    "metadata": {}
  }'
```

Validation error response (`400 Bad Request`):

```json
{
  "timestamp": "2026-06-22T18:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "fields": {
    "recipient": "Recipient is required",
    "channel": "Channel is required"
  }
}
```

Unauthorized response (`401 Unauthorized`):

```json
{
  "error": "Unauthorized",
  "message": "Missing or invalid X-API-Key header."
}
```

## 📂 Project Structure and Coding Standards

Coding styles, architecture designs, and patterns are documented in the reference guides:

- [ARCHITECTURE.md](ARCHITECTURE.md): component diagrams, sequence flows, database entity relationships, retry logic, security model, and WebSocket event lifecycle.
