/**
 * WebSocket 连接管理模块
 * 职责：维护与后端的 WebSocket 长连接，处理消息收发、断线重连、待发送队列
 *
 * 增强功能：
 * 1. 支持 VITE_WS_URL 环境变量配置地址
 * 2. 根据页面协议自动选择 ws 或 wss
 * 3. close() 后不再自动重连（手动关闭 vs 意外断开区分）
 * 4. 连接未建立时消息存入待发送队列，连接成功后统一发送
 * 5. 内部状态防重复连接
 *
 * 数据流：
 * 1. 接收后端广播的 STATE_UPDATE 消息 -> 通过 EventBus 通知前端组件更新
 * 2. 接收前端事件 -> 通过 WebSocket 发送操作消息到后端（未连接时放入队列）
 */
import { eventBus, EVENTS } from './EventBus';
import { WsMessage, QueueState } from './types';

/**
 * 构建 WebSocket 连接地址
 * 优先级：VITE_WS_URL 环境变量 > 根据当前页面协议/主机自动推断
 * 自动推断时开发环境使用 localhost:8080，生产环境使用当前 host
 */
function buildWsUrl(): string {
  const envUrl = (import.meta as any).env?.VITE_WS_URL as string | undefined;
  if (envUrl) {
    return envUrl;
  }

  const isHttps = window.location.protocol === 'https:';
  const protocol = isHttps ? 'wss:' : 'ws:';
  const isDev = (import.meta as any).env?.DEV as boolean;

  if (isDev) {
    return `${protocol}//localhost:8080/ws`;
  }

  return `${protocol}//${window.location.host}/ws`;
}

class WebSocketClient {
  private ws: WebSocket | null = null;
  private reconnectTimer: number | null = null;
  private url: string;
  private manuallyClosed: boolean = false;
  private connecting: boolean = false;
  private readonly pendingQueue: WsMessage[] = [];
  private readonly RECONNECT_DELAY = 3000;

  constructor() {
    this.url = buildWsUrl();
  }

  /**
   * 建立 WebSocket 连接
   * 防止重复连接：已连接或正在连接时直接返回
   */
  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN || this.connecting) {
      return;
    }
    this.manuallyClosed = false;
    this.connecting = true;

    try {
      this.ws = new WebSocket(this.url);
      console.log('[WebSocket] 正在连接: ' + this.url);

      this.ws.onopen = () => {
        this.connecting = false;
        console.log('[WebSocket] 连接成功');
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer);
          this.reconnectTimer = null;
        }
        this.flushPendingQueue();
        this.send({ action: 'GET_STATE' });
      };

      this.ws.onmessage = (event) => {
        try {
          const message: WsMessage = JSON.parse(event.data);
          this.handleMessage(message);
        } catch (e) {
          console.error('[WebSocket] 解析消息失败:', e);
        }
      };

      this.ws.onclose = (event) => {
        this.connecting = false;
        console.log(
          `[WebSocket] 连接关闭 (code=${event.code}), 手动关闭=${this.manuallyClosed}`
        );
        if (!this.manuallyClosed) {
          this.scheduleReconnect();
        }
      };

      this.ws.onerror = (error) => {
        this.connecting = false;
        console.error('[WebSocket] 连接错误:', error);
      };
    } catch (e) {
      this.connecting = false;
      console.error('[WebSocket] 创建连接失败:', e);
      if (!this.manuallyClosed) {
        this.scheduleReconnect();
      }
    }
  }

  /**
   * 调度自动重连（仅在非手动关闭时）
   */
  private scheduleReconnect(): void {
    if (this.manuallyClosed || this.reconnectTimer) {
      return;
    }
    console.log(`[WebSocket] ${this.RECONNECT_DELAY / 1000}秒后自动重连...`);
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, this.RECONNECT_DELAY);
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(message: WsMessage): void {
    switch (message.action) {
      case 'STATE_UPDATE':
        const state = message.payload as QueueState;
        eventBus.emit(EVENTS.QUEUE_STATE_UPDATED, state);
        break;
      default:
        console.log('[WebSocket] 收到未知消息:', message);
    }
  }

  /**
   * 发送消息
   * 连接已建立：直接发送
   * 连接未建立：放入待发送队列，连接成功后统一 flush
   */
  send(message: WsMessage): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      try {
        this.ws.send(JSON.stringify(message));
      } catch (e) {
        console.error('[WebSocket] 发送消息失败，放入队列:', e);
        this.pendingQueue.push(message);
      }
    } else {
      console.warn('[WebSocket] 连接未就绪，消息暂存队列:', message.action);
      this.pendingQueue.push(message);
    }
  }

  /**
   * 刷新待发送队列（连接成功后调用）
   */
  private flushPendingQueue(): void {
    if (this.pendingQueue.length === 0) {
      return;
    }
    console.log(`[WebSocket] 刷新待发送队列，共 ${this.pendingQueue.length} 条`);
    while (this.pendingQueue.length > 0) {
      const msg = this.pendingQueue.shift();
      if (msg && this.ws && this.ws.readyState === WebSocket.OPEN) {
        try {
          this.ws.send(JSON.stringify(msg));
        } catch (e) {
          console.error('[WebSocket] flush 时发送失败:', e, msg);
        }
      }
    }
  }

  /**
   * 手动关闭连接（设置标记，不再自动重连）
   */
  close(): void {
    this.manuallyClosed = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.ws) {
      try {
        this.ws.close();
      } catch (e) {
        console.error('[WebSocket] 关闭连接异常:', e);
      }
      this.ws = null;
    }
    this.connecting = false;
    this.pendingQueue.length = 0;
    console.log('[WebSocket] 已手动关闭连接');
  }
}

export const wsClient = new WebSocketClient();
