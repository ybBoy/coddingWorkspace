/**
 * EventBus 事件总线
 * 职责：前端组件之间的轻量级通信机制，替代全局变量
 * 采用发布-订阅模式，组件可以发布事件或订阅事件
 *
 * 数据流：用户操作 -> 组件发布事件 -> App.tsx 订阅事件 -> 通过 WebSocket 发送到后端
 */
type EventCallback = (data?: any) => void;

class EventBus {
  private listeners: Map<string, Set<EventCallback>> = new Map();

  on(event: string, callback: EventCallback): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(callback);
    return () => this.off(event, callback);
  }

  off(event: string, callback: EventCallback): void {
    const callbacks = this.listeners.get(event);
    if (callbacks) {
      callbacks.delete(callback);
    }
  }

  emit(event: string, data?: any): void {
    const callbacks = this.listeners.get(event);
    if (callbacks) {
      callbacks.forEach((cb) => cb(data));
    }
  }
}

export const eventBus = new EventBus();

export const EVENTS = {
  TAKE_TICKET: 'take-ticket',
  CALL_NEXT: 'call-next',
  COMPLETE_TICKET: 'complete-ticket',
  MISS_TICKET: 'miss-ticket',
  RECALL_TICKET: 'recall-ticket',
  SELECT_COUNTER: 'select-counter',
  QUEUE_STATE_UPDATED: 'queue-state-updated',
} as const;
