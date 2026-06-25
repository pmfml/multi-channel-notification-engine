export interface NotificationRequest {
  recipient: string;
  message: string;
  channel: 'EMAIL' | 'SMS' | 'PUSH';
  metadata?: Record<string, string>;
}

export const mcneApi = {
  sendNotification: async (request: NotificationRequest): Promise<void> => {
    const response = await fetch('http://localhost:8081/api/v1/notifications', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-MCNE-Client': 'Visualizer',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`API Error: ${response.statusText}`);
    }
  },
  
  reprocessDlq: async (): Promise<{ message: string }> => {
    const response = await fetch('http://localhost:8081/api/v1/notifications/dlq/reprocess', {
      method: 'POST',
      headers: {
        'X-MCNE-Client': 'Visualizer',
      },
    });

    if (!response.ok) {
      throw new Error(`API Error: ${response.statusText}`);
    }
    
    return response.json();
  },

  setConcurrency: async (count: number) => {
    const response = await fetch(`http://localhost:8081/api/v1/config/concurrency?count=${count}`, {
      method: 'PUT',
      headers: {
        'X-MCNE-Client': 'Visualizer'
      }
    });
    return response.ok;
  }
};
