import { eventBus, SeatData, SeatActionData, StatsData } from './EventBus';

const WS_URL = `ws://${window.location.hostname}:8081`;
const ADMIN_TOKEN = 'studyroom-admin-2026';

let ws: WebSocket | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let reconnectCount = 0;
let forwardingSetup = false;

let lastInitData: {
  seats: SeatData[];
  actions: SeatActionData[];
  broadcast: string;
  broadcastTimestamp: number;
} | null = null;

let lastBroadcastData: { message: string; timestamp: number } | null = null;

function connect(): void {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return;
  }

  ws = new WebSocket(WS_URL);

  ws.onopen = () => {
    reconnectCount = 0;
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
          lastInitData = {
            seats: msg.seats as SeatData[],
            actions: msg.actions as SeatActionData[],
            broadcast: msg.broadcast || '',
            broadcastTimestamp: msg.broadcastTimestamp || 0,
          };
          eventBus.emit('state:init', lastInitData);
          break;
        case 'update':
          if (lastInitData) {
            lastInitData.seats = msg.seats as SeatData[];
            lastInitData.actions = msg.actions as SeatActionData[];
          }
          eventBus.emit('state:update', {
            seats: msg.seats as SeatData[],
            actions: msg.actions as SeatActionData[],
          });
          break;
        case 'error':
          eventBus.emit('ws:error', msg.message as string);
          eventBus.emit('toast:show', { message: msg.message, type: 'error' });
          break;
        case 'broadcast':
          lastBroadcastData = {
            message: msg.message || '',
            timestamp: msg.timestamp || 0,
          };
          eventBus.emit('broadcast:update', lastBroadcastData);
          break;
        case 'adminLoginResult':
          eventBus.emit('admin:loginResult', { success: !!msg.success });
          break;
        case 'stats':
          eventBus.emit('admin:stats', msg.data as StatsData);
          break;
        case 'exportActions':
          eventBus.emit('admin:export', msg.actions as SeatActionData[]);
          break;
      }
    } catch (e) {
      console.error('Failed to parse ws message:', e);
    }
  };
}

function scheduleReconnect(): void {
  if (reconnectTimer) clearTimeout(reconnectTimer);
  reconnectCount++;
  const delay = Math.min(3000 * Math.pow(1.5, reconnectCount - 1), 30000);
  reconnectTimer = setTimeout(() => {
    connect();
  }, delay);
}

function setupEventBusForwarding(): void {
  if (forwardingSetup) return;
  forwardingSetup = true;

  eventBus.on<{ seatId: number; nickname: string }>('seat:sit', (data) => {
    sendAction('sit', { seatId: data.seatId, nickname: data.nickname });
  });
  eventBus.on<{ seatId: number; nickname: string }>('seat:away', (data) => {
    sendAction('away', { seatId: data.seatId, nickname: data.nickname });
  });
  eventBus.on<{ seatId: number; nickname: string }>('seat:leave', (data) => {
    sendAction('leave', { seatId: data.seatId, nickname: data.nickname });
  });
  eventBus.on<{ seatId: number; isAdmin?: boolean }>('seat:forceRelease', (data) => {
    const payload: Record<string, any> = { seatId: data.seatId };
    if (data.isAdmin) {
      payload.token = ADMIN_TOKEN;
    }
    sendAction('forceRelease', payload);
  });
  eventBus.on<{ token: string }>('admin:login', (data) => {
    sendAction('adminLogin', { token: data.token });
  });
  eventBus.on<{ message: string }>('admin:broadcast', (data) => {
    sendAction('broadcast', { message: data.message, token: ADMIN_TOKEN });
  });
  eventBus.on<void>('admin:requestStats', () => {
    sendAction('getStats', { token: ADMIN_TOKEN });
  });
  eventBus.on<void>('admin:requestExport', () => {
    sendAction('exportActions', { token: ADMIN_TOKEN });
  });
}

function replayLastState(): void {
  if (lastInitData) {
    eventBus.emit('state:init', lastInitData);
  }
  if (lastBroadcastData && lastBroadcastData.message) {
    eventBus.emit('broadcast:update', lastBroadcastData);
  }
}

export function initSocket(): void {
  connect();
  setupEventBusForwarding();
  if (getConnected()) {
    replayLastState();
  }
}

export function sendAction(type: string, payload: Record<string, any>): void {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type, ...payload }));
  } else {
    eventBus.emit('ws:error', '未连接到服务器，请稍后重试');
    eventBus.emit('toast:show', { message: '未连接到服务器', type: 'warning' });
  }
}

export function getConnected(): boolean {
  return ws !== null && ws.readyState === WebSocket.OPEN;
}
