import { Client, IMessage } from '@stomp/stompjs';

export interface WebSocketNotificationEvent {
  logId: string;
  eventType: string; // QUEUED, PROCESSING, RETRYING, SENT, DLQ
  channel: string;
  message: string;
}

type EventCallback = (event: WebSocketNotificationEvent) => void;

class WebSocketService {
  private client: Client;
  private onEventReceived?: EventCallback;

  constructor() {
    this.client = new Client({
      brokerURL: 'ws://localhost:8081/ws-mcne',
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('Connected to MCNE WebSocket!');
        this.client.subscribe('/topic/notifications', (message: IMessage) => {
          if (this.onEventReceived) {
            const event: WebSocketNotificationEvent = JSON.parse(message.body);
            this.onEventReceived(event);
          }
        });
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      },
    });
  }

  public connect(callback: EventCallback) {
    this.onEventReceived = callback;
    this.client.activate();
  }

  public disconnect() {
    this.client.deactivate();
  }
}

export const wsService = new WebSocketService();
