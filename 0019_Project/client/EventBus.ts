/**
 * 事件总线：前端组件之间通过它来传递事件，避免全局变量泛滥。
 * 采用发布-订阅模式，任意组件可以 emit 事件，其他组件可以 on 监听。
 */
type EventCallback = (data?: any) => void;

class EventBus {
  private listeners: Record<string, EventCallback[]> = {};

  on(event: string, callback: EventCallback): void {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(callback);
  }

  off(event: string, callback: EventCallback): void {
    if (!this.listeners[event]) return;
    this.listeners[event] = this.listeners[event].filter((cb) => cb !== callback);
  }

  emit(event: string, data?: any): void {
    if (!this.listeners[event]) return;
    this.listeners[event].forEach((cb) => cb(data));
  }
}

export const eventBus = new EventBus();

export const EVENTS = {
  VOTE: 'vote',
  ADD_OPTION: 'add_option',
  CLEAR_ALL: 'clear_all',
  DATA_UPDATED: 'data_updated',
  WS_CONNECTED: 'ws_connected',
  WS_ERROR: 'ws_error',
};
