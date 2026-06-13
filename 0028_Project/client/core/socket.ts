import { eventBus, SeatData, SeatActionData } from './EventBus';

const WS_URL = `ws://${window.location.hostname}:8081`;

let ws: WebSocket | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

function connect(): void {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return;
  }

  ws = new WebSocket(WS_URL);

  ws.onopen = () => {
    eventBus.emit('ws:connected');
  };

  ws.onclose = () => {
    eventBus.emit('ws:disconnected');
    scheduleReconnect();
  };

  ws.onerror = () => {
    eventBus.emit('ws:error', 'WebSocket connection error');
  };

  ws.onmessage = (event: MessageEvent) => {
    try {
      const msg = JSON.parse(event.data);
      switch (msg.type) {
        case 'init':
          eventBus.emit('state:init', {
            seats: msg.seats as SeatData[],
            actions: msg.actions as SeatActionData[],
          });
          break;
        case 'update':
          eventBus.emit('state:update', {
            seats: msg.seats as SeatData[],
            actions: msg.actions as SeatActionData[],
          });
          break;
        case 'error':
          eventBus.emit('ws:error', msg.message as string);
          break;
      }
    } catch (e) {
      console.error('Failed to parse ws message:', e);
    }
  };
}

function scheduleReconnect(): void {
  if (reconnectTimer) clearTimeout(reconnectTimer);
  reconnectTimer = setTimeout(() => {
    connect();
  }, 3000);
}

export function initSocket(): void {
  connect();
}

export function sendAction(type: string, payload: Record<string, any>): void {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type, ...payload }));
  } else {
    eventBus.emit('ws:error', '未连接到服务器，请稍后重试');
  }
}

export function getConnected(): boolean {
  return ws !== null && ws.readyState === WebSocket.OPEN;
}
