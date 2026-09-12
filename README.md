# Multi-Channel Notification Engine (MCNE) ✉️📱🔔

**Multi-Channel Notification Engine (MCNE)** is a high-throughput backend service designed to centralize and standardize the delivery of notifications across multiple communication channels (Email, SMS, Push Notifications, Webhooks, and more).

Acting as a central gateway in a microservices architecture, it allows different business domains to trigger outbound communication asynchronously without integrating directly with third-party providers or dealing with network retry logic.

## 🚀 Key Features

> **All features listed below are fully implemented and verified via unit, slice, and integration test suites.**

- ✅ **Extensible Strategy + Template Method Architecture:** Dynamic channel routing adhering to the Open/Closed Principle (OCP). Adding a new channel (e.g., Push, Webhooks, Slack) requires only a single class extending `AbstractNotificationStrategy` without modifying existing engine logic.
- ✅ **High-Concurrency Virtual Threads:** Powered by Java 21 Project Loom virtual threads (`spring.threads.virtual.enabled=true`) for non-blocking, lightweight consumer thread scaling during network I/O.
- ✅ **Asynchronous Message Broker:** Event-driven core using RabbitMQ exchanges and queues to decouple HTTP intake from slow third-party provider calls with immediate `202 Accepted` receipts.
- ✅ **Multi-Tiered Fault Tolerance & DLQ:** Configurable exponential backoff via Spring Retry targeting transient network errors (`SdkClientException`). Failed deliveries automatically route to `notification.dlq` to prevent poison-pill congestion.
- ✅ **Automated DLQ Reprocessing:** Replay API (`POST /api/v1/notifications/dlq/reprocess`) to bulk-drain and re-enqueue dead-lettered notifications after external provider outages resolve.
- ✅ **Real-Time WebSocket Pipeline Observability:** Non-blocking STOMP WebSocket broadcast over `/topic/notifications` publishing state transitions (`RECEIVED`, `QUEUED`, `PROCESSING`, `RETRYING`, `SENT`, `DLQ`) to connected dashboards.
- ✅ **Stateless API Key Authentication:** Centralized Spring Security filter enforcing `X-API-Key` headers on mutation endpoints while keeping health probes and WebSocket handshakes open.
- ✅ **Zero-Leakage WebSocket Privacy:** Sensitive payload bodies and recipient contact details are protected in production broadcasts, exposing message text only when the isolated `demo` profile is active.
- ✅ **Versioned Schema Migrations:** PostgreSQL schema versioning and auditing managed by Flyway migrations with strict Hibernate validation (`ddl-auto=validate`).
- ✅ **Interactive Visualizer Frontend:** React 19 + TypeScript + Tailwind CSS + Framer Motion visualizer demonstrating message flow, consumer concurrency scaling, simulated AWS errors, and batch workloads.

## 🛠️ Technology Stack

- **Language:** Java 21 (Virtual Threads, Records, Pattern Matching)
- **Framework:** Spring Boot 3.5.14 with Spring Data JPA & Spring Security
- **API Documentation:** OpenAPI 3 / Swagger UI
- **Messaging Broker:** RabbitMQ 3.13 (AMQP protocol + Management Console)
- **Resilience:** Spring Retry with exponential backoff & Dead Letter Queue (DLQ)
- **Real-Time Broadcast:** Spring WebSocket + STOMP over SockJS
- **Database:** PostgreSQL 16 (schema managed by Flyway migrations)
- **Cloud Providers:** AWS SES (Email), AWS SNS (SMS) via AWS SDK v2
- **Frontend:** React 19, TypeScript, Vite 8, Tailwind CSS, Framer Motion, STOMP.js
- **Testing:** JUnit 5, Mockito, Spring WebMvcTest, H2 in-memory test database
- **Containerization:** Docker / Docker Compose

## 📋 Prerequisites

To run this application locally, you will need:

1. Java JDK 21 or higher.
2. Docker and Docker Compose installed and running.
3. Maven 3.8+ (or use the included `./mvnw` wrapper).

## ⚙️ How to Get Started

### 1. Clone the repository

```bash
git clone https://github.com/pmfml/multi-channel-notification-engine.git
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

## 🖥️ Running the Visualizer (Frontend)

The project ships with a React + Vite frontend that visually demonstrates the asynchronous pipeline in real time: messages flowing through the API, RabbitMQ, the consumer, and on to delivery or the DLQ, plus a live event terminal and interactive controls (single send, batch simulator, failure injection, consumer concurrency, and DLQ reprocessing).

### 1. Start the backend in demo mode

The Visualizer relies on the `demo` profile to enable the animated delays and the "Force AWS Error" simulation (this behaviour is intentionally inactive in production):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

### 2. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

The Visualizer opens at `http://localhost:5173`.

### Configuration

The frontend works out of the box with no configuration: it talks to `http://localhost:8081` and authenticates with the default API key (`dev-only-key`). To point it at a different backend or key, copy `frontend/.env.example` to `frontend/.env` and adjust:

| Variable | Description | Default |
| :--- | :--- | :--- |
| `VITE_API_URL` | Backend REST base URL | `http://localhost:8081` |
| `VITE_WS_URL` | Backend WebSocket (STOMP) URL | `ws://localhost:8081/ws-mcne` |
| `VITE_API_KEY` | API key sent via `X-API-Key` (must match backend `MCNE_API_KEY`) | `dev-only-key` |

> If you set a custom `MCNE_API_KEY` on the backend, set the same value in `VITE_API_KEY` so the Visualizer stays authenticated. The `5173` origin is already allowed by the default CORS configuration.

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

## 📡 REST API Documentation & Interactive UI

The engine features interactive API documentation provided by **OpenAPI (Swagger UI)**. 

Once the application is running, navigate to [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) to explore the endpoints, view schemas, and test requests directly from your browser. 
> **Note:** Click the green **Authorize** button in the top right corner to input your `X-API-Key` before testing the protected endpoints.

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

## 🔍 Observing the Pipeline Without the Frontend

The Visualizer is optional. The full asynchronous lifecycle can be observed using only backend tooling: the RabbitMQ Management UI, the application logs, and the database. This is the recommended path to inspect the internals.

### 1. RabbitMQ Management UI

Open `http://localhost:15672` (default credentials: `guest` / `guest`) and go to the **Queues** tab. Two queues tell the whole story:

| Queue | What to watch |
| :--- | :--- |
| `notification.queue` | Main work queue. The "Ready" and "Unacked" counters spike when you publish and drain as the consumer processes messages. |
| `notification.dlq` | Dead Letter Queue. Messages land here after all retries are exhausted. |

Useful actions in the UI:
- Watch the message-rate graphs on `notification.queue` while sending a burst of `curl` requests.
- Click `notification.dlq` and use **Get messages** to inspect a failed payload without removing it.
- Pause processing by scaling the consumer to zero (`PUT /api/v1/config/concurrency?count=0`) and watch messages pile up as "Ready"; then scale back up and watch them drain.

### 2. Application Logs

The console logs narrate each stage (`@Slf4j`), so a `tail` of the running app shows the same lifecycle the Visualizer animates:

```
Message received for log ID: 6f1c...      # consumed from the queue
Sending EMAIL to: user@example.com         # strategy invoked
EMAIL successfully sent via AWS SES ...     # delivered  (or)
Demo mode: simulating AWS SES error ...     # forced failure -> retry -> DLQ
```

### 3. Database (final state)

Every notification is persisted in the `notification_log` table, the source of truth for the final status:

```bash
docker exec -it mcne-postgres psql -U mcne_user -d mcne_db \
  -c "SELECT id, channel, status, created_at FROM notification_log ORDER BY created_at DESC LIMIT 10;"
```

The `status` column moves through `PENDING` then `SENT` or `FAILED`.

### 4. Triggering a failure to see the DLQ (curl only)

Run the backend in `demo` mode and send a request flagged to simulate a provider error. It will exhaust its retries and be routed to `notification.dlq`:

```bash
curl -X POST http://localhost:8081/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-only-key" \
  -H "X-MCNE-Client: Visualizer" \
  -d '{
    "recipient": "user@example.com",
    "message": "This one will fail",
    "channel": "EMAIL",
    "metadata": { "simulateError": "true" }
  }'
```

Watch `notification.dlq` grow in the Management UI, then replay every dead-lettered message back onto the main exchange:

```bash
curl -X POST http://localhost:8081/api/v1/notifications/dlq/reprocess \
  -H "X-API-Key: dev-only-key"
# -> {"message":"1 messages reprocessed successfully."}
```

---

## 📂 Project Structure

```
multi-channel-notification-engine/
├── docker-compose.yml              # Local infrastructure (PostgreSQL 16, RabbitMQ 3.13 + Management)
├── Dockerfile                      # Multi-stage production container build
├── pom.xml                         # Maven reactor & dependencies (Spring Boot 3.5.14, AWS SDK v2)
├── docs/
│   └── ARCHITECTURE.md             # In-depth architectural design, sequence flows, ER model & OCP patterns
├── frontend/                       # React 19 Visualizer SPA (Tailwind CSS, Framer Motion, STOMP)
│   ├── README.md                   # Visualizer frontend architecture and setup guide
│   ├── src/
│   │   ├── components/             # VisualPipeline, ControlPanel, EventLogTerminal, StatusSummary
│   │   ├── services/               # REST client & STOMP WebSocket client
│   │   └── types/                  # TypeScript definitions for events and channels
│   └── vite.config.ts              # Vite configuration
├── test-scripts/                   # Bash scripts for burst testing and automated demo verification
└── src/                            # Spring Boot application
    ├── main/java/com/pmfml/mcne/
    │   ├── config/                 # RabbitMQ exchanges/queues, Security API key filter, WebSockets, AWS
    │   ├── controllers/            # REST controllers (/api/v1/notifications, config, status)
    │   ├── dtos/                   # Immutable Java record DTOs for requests/responses
    │   ├── entities/               # JPA entity (NotificationLog) and enums (Channel, Status, EventType)
    │   ├── exceptions/             # GlobalExceptionHandler & custom exception classes
    │   ├── listeners/              # RabbitMQ consumers processing messages asynchronously
    │   ├── services/               # Dispatcher, strategies (SES, SNS, Abstract), and WebSocket publisher
    │   └── strategies/             # Concrete channel implementations extending AbstractNotificationStrategy
    ├── main/resources/
    │   ├── db/migration/           # Flyway SQL migrations for versioned database schema
    │   └── application.properties  # Application configuration with environment variable defaults
    └── test/                       # Unit and slice test suites (JUnit 5, Mockito, MockMvc, H2)
```

For complete architectural details, entity relationships, and sequence diagrams, refer to [ARCHITECTURE.md](docs/ARCHITECTURE.md). For frontend setup and test execution, refer to [frontend/README.md](frontend/README.md).
