// Base URL and API key are configurable via Vite env vars, with sensible
// defaults so the Visualizer runs out of the box against a local backend
// started with the `dev-only-key` default.
const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8081';
const API_KEY = import.meta.env.VITE_API_KEY ?? 'dev-only-key';

// Shared headers sent on every request: identifies the Visualizer client
// (enables demo behaviour when the backend runs with the `demo` profile) and
// authenticates via the API key.
const baseHeaders: Record<string, string> = {
  'X-MCNE-Client': 'Visualizer',
  'X-API-Key': API_KEY,
};

export interface NotificationRequest {
  recipient: string;
  message: string;
  channel: 'EMAIL' | 'SMS' | 'PUSH';
  metadata?: Record<string, string>;
}

export const mcneApi = {
  sendNotification: async (request: NotificationRequest): Promise<void> => {
    const response = await fetch(`${API_BASE}/api/v1/notifications`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...baseHeaders,
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`API Error: ${response.statusText}`);
    }
  },

  reprocessDlq: async (): Promise<{ message: string }> => {
    const response = await fetch(`${API_BASE}/api/v1/notifications/dlq/reprocess`, {
      method: 'POST',
      headers: { ...baseHeaders },
    });

    if (!response.ok) {
      throw new Error(`API Error: ${response.statusText}`);
    }

    return response.json();
  },

  setConcurrency: async (count: number) => {
    const response = await fetch(`${API_BASE}/api/v1/config/concurrency?count=${count}`, {
      method: 'PUT',
      headers: { ...baseHeaders },
    });
    return response.ok;
  },
};
