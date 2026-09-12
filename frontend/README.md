# MCNE Visualizer - Frontend 🖥️⚡

Interactive Real-Time Pipeline Visualizer for the **Multi-Channel Notification Engine (MCNE)**, built with React 19, TypeScript, Vite 8, Tailwind CSS, and Framer Motion.

---

## 🚀 Overview & Features

The Visualizer provides live visual observability of the asynchronous message pipeline, allowing you to watch messages travel through each architectural stage in real time:

- **Animated Pipeline Visualization:** Animated flow representing stages: Client $\to$ REST API $\to$ RabbitMQ Broker $\to$ Consumer Worker $\to$ External Provider (SES / SNS) or Dead Letter Queue (DLQ).
- **STOMP WebSocket Integration:** Real-time event consumption via STOMP over SockJS (`/topic/notifications`), subscribing to asynchronous status transitions: `RECEIVED`, `QUEUED`, `PROCESSING`, `RETRYING`, `SENT`, `DLQ`.
- **Interactive Control Panel:**
  - **Single Dispatch Form:** Custom recipient, message text, and channel selection (Email or SMS).
  - **Batch Simulator:** High-throughput burst generator (e.g., 10, 25, or 50 concurrent requests) to demonstrate asynchronous queueing and consumer drain.
  - **Failure Injection Mode:** Simulate external AWS provider network outages to trigger exponential retries and Dead Letter Queue isolation.
  - **Consumer Concurrency Slider:** Dynamically scale consumer workers (0 to 10) in real time via the backend concurrency API (`PUT /api/v1/config/concurrency`).
  - **DLQ Reprocessing Trigger:** Trigger one-click bulk replay of failed messages back into the main exchange.
- **Terminal Event Log:** Live scrolling terminal displaying formatted timestamps, notification IDs, channels, and event types.

---

## 🛠️ Tech Stack

- **Framework:** React 19 + TypeScript
- **Build Tool:** Vite 8
- **Styling:** Tailwind CSS
- **Animations:** Framer Motion
- **Messaging Client:** `@stomp/stompjs` + `sockjs-client`
- **HTTP Client:** Native Fetch with API key header injection

---

## ⚙️ Getting Started

### Prerequisites
- Node.js 20+ (or LTS)
- Running MCNE backend (default: `http://localhost:8081` in `demo` profile)

### Installation
```bash
npm install
```

### Running Development Server
```bash
npm run dev
```
The Visualizer UI will open at `http://localhost:5173`.

### Environment Configuration
The frontend connects by default to `http://localhost:8081` and `ws://localhost:8081/ws-mcne`. To customize endpoints or API keys, copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

| Variable | Description | Default |
| :--- | :--- | :--- |
| `VITE_API_URL` | Backend REST base URL | `http://localhost:8081` |
| `VITE_WS_URL` | Backend WebSocket (STOMP) URL | `ws://localhost:8081/ws-mcne` |
| `VITE_API_KEY` | API Key sent via `X-API-Key` | `dev-only-key` |

### Production Build
```bash
npm run build
```
Build assets will be emitted to `dist/`.

---

## 📂 Component Architecture

```
frontend/
├── src/
│   ├── components/
│   │   ├── ControlPanel.tsx       # Dispatch form, batch simulator & failure injection toggles
│   │   ├── EventLogTerminal.tsx   # Live scrolling event terminal
│   │   ├── StatusSummary.tsx      # Metrics counters (sent, queued, retrying, dlq)
│   │   └── VisualPipeline.tsx     # Framer Motion animated node pipeline
│   ├── services/
│   │   ├── api.ts                 # REST API client with API key authorization
│   │   └── websocket.ts           # STOMP / SockJS WebSocket client subscription
│   ├── types/
│   │   └── index.ts               # Shared TypeScript interfaces for notifications & events
│   ├── App.tsx                    # Root layout & state coordinator
│   └── main.tsx                   # React root entry point
├── vite.config.ts                 # Vite bundler configuration
└── package.json                   # Dependencies and scripts
```
