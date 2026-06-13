import { eventBus, Events } from './EventBus';
import type {
  Room,
  RoomLog,
  WsMessage,
  AlertItem,
  Operator,
  RoomDetail,
  FullSyncPayload,
} from './types';

class WsClient {
  private ws: WebSocket | null = null;
  private url: string;
  private reconnectTimer: number | null = null;
  private reconnectDelay = 3000;
  private rooms: Room[] = [];
  private logs: RoomLog[] = [];
  private alerts: AlertItem[] = [];
  private operators: Operator[] = [];
  private currentOperator: string = '前台';

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
      case 'FULL_SYNC': {
        const payload = message.payload as FullSyncPayload;
        this.rooms = payload.rooms || [];
        this.logs = payload.logs || [];
        this.alerts = payload.alerts || [];
        this.operators = payload.operators || [];
        this.currentOperator = payload.currentOperator || '前台';
        eventBus.emit(Events.ROOMS_UPDATED, this.rooms);
        eventBus.emit('logs:updated', this.logs);
        eventBus.emit(Events.ALERTS_UPDATED, this.alerts);
        eventBus.emit(Events.OPERATORS_UPDATED, this.operators);
        eventBus.emit(Events.OPERATOR_CHANGED, this.currentOperator);
        break;
      }
      case 'ROOMS_UPDATE':
        this.rooms = message.payload.rooms || [];
        eventBus.emit(Events.ROOMS_UPDATED, this.rooms);
        break;
      case 'LOGS_UPDATE':
        this.logs = message.payload.logs || [];
        eventBus.emit('logs:updated', this.logs);
        break;
      case 'ALERTS_UPDATE':
        this.alerts = message.payload.alerts || [];
        eventBus.emit(Events.ALERTS_UPDATED, this.alerts);
        break;
      case 'ROOM_DETAIL': {
        const detail = message.payload as RoomDetail;
        eventBus.emit(Events.ROOM_DETAIL, detail);
        break;
      }
      case 'OPERATORS_LIST':
        this.operators = message.payload.operators || [];
        this.currentOperator = message.payload.currentOperator || '前台';
        eventBus.emit(Events.OPERATORS_UPDATED, this.operators);
        eventBus.emit(Events.OPERATOR_CHANGED, this.currentOperator);
        break;
      case 'EXPORT_DATA': {
        const { filename, content } = message.payload;
        this.downloadFile(filename, content, 'text/csv;charset=utf-8');
        break;
      }
    }
  }

  private downloadFile(filename: string, content: string, mimeType: string): void {
    const blob = new Blob(['\uFEFF' + content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
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

  getAlerts(): AlertItem[] {
    return this.alerts;
  }

  getOperators(): Operator[] {
    return this.operators;
  }

  getCurrentOperator(): string {
    return this.currentOperator;
  }

  setCurrentOperator(name: string): void {
    this.currentOperator = name;
    this.send('SET_OPERATOR', { operatorName: name });
    eventBus.emit(Events.OPERATOR_CHANGED, name);
  }

  requestRoomDetail(roomId: string): void {
    this.send('GET_ROOM_DETAIL', { roomId });
  }

  requestOperators(): void {
    this.send('GET_OPERATORS', {});
  }

  exportStayRecords(): void {
    this.send('EXPORT_STAY_RECORDS', {});
  }

  exportLogs(): void {
    this.send('EXPORT_LOGS', {});
  }

  batchCleanByFloor(floor: number): void {
    this.send('BATCH_CLEAN_BY_FLOOR', { floor });
  }

  batchMarkDirtyByFloor(floor: number): void {
    this.send('BATCH_MARK_DIRTY_BY_FLOOR', { floor });
  }

  addRoom(roomNo: string, floor: number, type: string, defaultPrice: number): void {
    this.send('ADD_ROOM', { roomNo, floor, type, defaultPrice });
  }

  updateRoom(
    roomId: string,
    roomNo: string,
    floor: number,
    type: string,
    defaultPrice: number,
  ): void {
    this.send('UPDATE_ROOM', { roomId, roomNo, floor, type, defaultPrice });
  }

  deleteRoom(roomId: string): void {
    this.send('DELETE_ROOM', { roomId });
  }

  disableRoom(roomId: string, remark?: string): void {
    this.send('DISABLE_ROOM', { roomId, remark });
  }

  enableRoom(roomId: string): void {
    this.send('ENABLE_ROOM', { roomId });
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
