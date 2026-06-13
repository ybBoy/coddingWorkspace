import { eventBus } from './EventBus';
import { AppState, ConnectionStatus } from './types';

class SocketService {
  private ws: WebSocket | null = null;
  private status: ConnectionStatus = 'disconnected';
  private reconnectTimer: number | null = null;
  private url: string = '';

  connect(host?: string): void {
    if (this.ws) {
      this.ws.close();
    }

    const isDev = process.env.NODE_ENV === 'development';
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    let wsHost = host || window.location.host;
    if (isDev && !host) {
      wsHost = 'localhost:8080';
    }
    this.url = `${protocol}//${wsHost}`;

    this.setStatus('connecting');

    try {
      this.ws = new WebSocket(this.url);

      this.ws.onopen = () => {
        this.setStatus('connected');
      };

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          this.handleMessage(data);
        } catch (e) {
          console.error('Failed to parse WebSocket message:', e);
        }
      };

      this.ws.onclose = () => {
        this.setStatus('disconnected');
        this.scheduleReconnect();
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
      };
    } catch (e) {
      console.error('Failed to create WebSocket:', e);
      this.setStatus('disconnected');
      this.scheduleReconnect();
    }
  }

  private setStatus(status: ConnectionStatus): void {
    this.status = status;
    eventBus.emit('connection-status', status);
  }

  getStatus(): ConnectionStatus {
    return this.status;
  }

  private handleMessage(data: any): void {
    switch (data.type) {
      case 'state':
        eventBus.emit('state-update', data as AppState);
        break;
      case 'host-granted':
        eventBus.emit('host-granted');
        break;
      case 'room-created':
        eventBus.emit('room-created', data);
        break;
      case 'self-registered':
        eventBus.emit('self-registered', data);
        break;
      case 'export-data':
        eventBus.emit('export-data', data);
        break;
      case 'error':
        eventBus.emit('error', data.message);
        break;
      case 'success':
        eventBus.emit('success', data.message);
        break;
      default:
        break;
    }
  }

  send(data: any): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data));
    } else {
      console.warn('WebSocket not connected, message not sent');
    }
  }

  createRoom(activityName: string): void {
    this.send({ type: 'create-room', activityName });
  }

  joinRoom(roomCode: string): void {
    this.send({ type: 'join-room', roomCode: roomCode.toUpperCase() });
  }

  selfRegister(roomCode: string, name: string, gender?: string, department?: string): void {
    this.send({ type: 'self-register', roomCode, name, gender, department });
  }

  claimHost(token: string): void {
    this.send({ type: 'claim-host', token });
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer);
    }
    this.reconnectTimer = window.setTimeout(() => {
      this.connect();
    }, 3000);
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}

export const socket = new SocketService();
export default socket;
