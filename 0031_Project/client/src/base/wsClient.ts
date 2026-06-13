import { eventBus, Events } from './EventBus';
import type { Room, RoomLog, WsMessage } from './types';

class WsClient {
  private ws: WebSocket | null = null;
  private url: string;
  private reconnectTimer: number | null = null;
  private reconnectDelay = 3000;
  private rooms: Room[] = [];
  private logs: RoomLog[] = [];

  constructor(url: string) {
    this.url = url;
  }

  connect(): void {
    try {
      this.ws = new WebSocket(this.url);

      this.ws.onopen = () => {
        console.log('[WS] Connected');
        eventBus.emit(Events.WS_CONNECTED);
        this.clearReconnectTimer();
      };

      this.ws.onmessage = (event) => {
        try {
          const message: WsMessage = JSON.parse(event.data);
          this.handleMessage(message);
        } catch (e) {
          console.error('[WS] Message parse error:', e);
        }
      };

      this.ws.onclose = () => {
        console.log('[WS] Disconnected');
        eventBus.emit(Events.WS_DISCONNECTED);
        this.scheduleReconnect();
      };

      this.ws.onerror = (error) => {
        console.error('[WS] Error:', error);
      };
    } catch (e) {
      console.error('[WS] Connection error:', e);
      this.scheduleReconnect();
    }
  }

  private handleMessage(message: WsMessage): void {
    switch (message.type) {
      case 'FULL_SYNC':
        this.rooms = message.payload.rooms || [];
        this.logs = message.payload.logs || [];
        eventBus.emit('rooms:updated', this.rooms);
        eventBus.emit('logs:updated', this.logs);
        break;
      case 'ROOMS_UPDATE':
        this.rooms = message.payload.rooms || [];
        eventBus.emit('rooms:updated', this.rooms);
        break;
      case 'LOGS_UPDATE':
        this.logs = message.payload.logs || [];
        eventBus.emit('logs:updated', this.logs);
        break;
    }
  }

  private scheduleReconnect(): void {
    this.clearReconnectTimer();
    this.reconnectTimer = window.setTimeout(() => {
      console.log('[WS] Reconnecting...');
      this.connect();
    }, this.reconnectDelay);
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  send(type: string, payload: any): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ type, payload }));
    } else {
      console.warn('[WS] Not connected, message dropped');
    }
  }

  getRooms(): Room[] {
    return this.rooms;
  }

  getLogs(): RoomLog[] {
    return this.logs;
  }

  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }

  disconnect(): void {
    this.clearReconnectTimer();
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}

const wsUrl =
  location.protocol === 'https:'
    ? `wss://${location.host}/ws`
    : `ws://${location.hostname}:8765/ws`;

export const wsClient = new WsClient(wsUrl);
