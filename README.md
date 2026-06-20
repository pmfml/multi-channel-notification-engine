# Multi-Channel Notification Engine (MCNE) ✉️📱🔔

**Multi-Channel Notification Engine (MCNE)** is a high-throughput backend service designed to centralize and standardize the delivery of notifications across multiple communication channels (Email, SMS, Webhooks, and Push Notifications).

Acting as a central gateway in a microservices architecture, it allows different business domains to trigger outbound communication asynchronously without integrating directly with third-party providers or dealing with network retry logic.

---

## 🚀 Key Features

*   **Strategy-Based Routing:** Uses the Strategy design pattern to dynamically route notification payloads to their respective channel providers.
*   **Asynchronous Core:** Leverages Spring AMQP (RabbitMQ) queues and Java 21 Virtual Threads to achieve high concurrent I/O throughput.
*   **Resiliency & Fault Tolerance:** Integrated with **Spring Retry** offering configurable exponential backoff. Failed deliveries are safely offloaded to a **Dead Letter Queue (DLQ)**.
*   **Provider Independent:** Swappable implementation for services such as AWS SES, Twilio, and external HTTP Webhooks.
*   **DLQ Recovery API:** Endpoint to trigger bulk reprocessing of dead-lettered notifications once external providers recover.
*   **Observability:** Health, database migrator (Flyway) checks, and system diagnostics exposed via Spring Boot Actuator.

---

## 🛠️ Technology Stack

*   **Language:** Java 21 (Virtual Threads, Records, Pattern Matching)
*   **Framework:** Spring Boot 3.5.x with Spring Data JPA
*   **Message Broker:** RabbitMQ
*   **Database:** PostgreSQL 16
*   **Migration Engine:** Flyway
*   **Local Containerization:** Docker / Docker Compose
*   **Testing:** JUnit 5, Mockito, Spring WebMvcTest

---

## 📋 Prerequisites

To run this application locally, you will need:

1.  **Java JDK 21** or higher.
2.  **Docker & Docker Compose** installed and running.
3.  **Maven 3.8+** (or use the included `./mvnw` wrapper).

---

## ⚙️ How to Get Started

### 1. Clone the repository
```bash
git clone https://github.com/<your-username>/multi-channel-notification-engine.git
cd multi-channel-notification-engine
```

### 2. Infrastructure Setup (Docker)
The project includes a `compose.yaml` file defining the PostgreSQL database (port **5432**) and RabbitMQ broker (management console on port **15672**).

To start the infrastructure services, run:
```bash
docker compose up -d
```
_Tip: Access the RabbitMQ Management UI at `http://localhost:15672` (credentials: `guest` / `guest`)._

### 3. Build & Run Tests
Execute the unit and integration testing suite to confirm system integrity:
```bash
./mvnw clean test
```

### 4. Running the Application
Start the Spring Boot notification server:
```bash
./mvnw spring-boot:run
```
The REST API will be available at `http://localhost:8081`.

---

## 📡 REST API Documentation

Below are the base endpoints available for Notification management:

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/status` | Retrieves the application health and uptime details | `200 OK` |
| **POST** | `/api/v1/notifications` | Submits an asynchronous notification dispatch request | `202 Accepted` |
| **POST** | `/api/v1/notifications/dlq/reprocess` | Retries sending all failed messages stored in the DLQ | `200 OK` |

### Sample HTTP Payloads

#### Dispatch Notification (`POST /api/v1/notifications`)
```json
{
  "recipient": "user@example.com",
  "message": "Welcome to our platform!",
  "channel": "EMAIL",
  "metadata": {}
}
```

#### DLQ Reprocessing Response (`POST /api/v1/notifications/dlq/reprocess`)
```json
{
  "message": "10 messages reprocessed successfully."
}
```

---

## 📂 Project Structure & Coding Standards

Coding styles, architecture designs, and patterns are documented in the reference guides:
*   [ARCHITECTURE.md](ARCHITECTURE.md): Component diagrams, sequence flows, database entity relationships, and retry logic.
