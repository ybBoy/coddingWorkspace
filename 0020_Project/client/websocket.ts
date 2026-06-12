/**
 * WebSocket 连接管理模块
 * 职责：维护与后端的 WebSocket 长连接，处理消息收发、断线重连
 *
 * 数据流：
 * 1. 接收后端广播的 STATE_UPDATE 消息 -> 通过 EventBus 通知前端组件更新
 * 2. 接收前端事件 -> 通过 WebSocket 发送操作消息到后端
 */
import { eventBus, EVENTS } from './EventBus';
import { WsMessage, QueueState } from './types';

class WebSocketClient {
  private ws: WebSocket | null = null;
  private reconnectTimer: number | null = null;
  private url: string;

  constructor(url: string) {
    this.url = url;
  }

  connect(): void {
    try {
      this.ws = new WebSocket(this.url);

      this.ws.onopen = () => {
        console.log('WebSocket 已连接');
        this.send({ action: 'GET_STATE' });
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer);
          this.reconnectTimer = null;
        }
      };

      this.ws.onmessage = (event) => {
        try {
          const message: WsMessage = JSON.parse(event.data);
          this.handleMessage(message);
        } catch (e) {
          console.error('解析 WebSocket 消息失败:', e);
        }
      };

      this.ws.onclose = () => {
        console.log('WebSocket 断开，3秒后重连...');
        this.scheduleReconnect();
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket 错误:', error);
      };
    } catch (e) {
      console.error('创建 WebSocket 失败:', e);
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect(): void {
    if (!this.reconnectTimer) {
      this.reconnectTimer = window.setTimeout(() => {
        this.connect();
      }, 3000);
    }
  }

  private handleMessage(message: WsMessage): void {
    switch (message.action) {
      case 'STATE_UPDATE':
        const state = message.payload as QueueState;
        eventBus.emit(EVENTS.QUEUE_STATE_UPDATED, state);
        break;
      default:
        console.log('收到未知消息:', message);
    }
  }

  send(message: WsMessage): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('WebSocket 未连接，消息丢失:', message);
    }
  }

  close(): void {
    if (this.ws) {
      this.ws.close();
    }
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }
  }
}

export const wsClient = new WebSocketClient(
  import.meta.env.DEV ? 'ws://localhost:5173/ws' : `ws://${window.location.host}/ws`
);
