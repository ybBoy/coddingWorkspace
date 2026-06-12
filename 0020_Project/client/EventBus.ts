/**
 * EventBus 事件总线
 * 职责：前端组件之间的轻量级通信机制，替代全局变量
 * 采用发布-订阅模式，组件可以发布事件或订阅事件
 *
 * 数据流：用户操作 -> 组件发布事件 -> App.tsx 订阅事件 -> 通过 WebSocket 发送到后端
 * 迭代新增：连接状态变更、Toast 推送、过号管理、窗口配置、业务类型筛选等事件
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
  CALL_NEXT_BY_TYPE: 'call-next-by-type',
  COMPLETE_TICKET: 'complete-ticket',
  MISS_TICKET: 'miss-ticket',
  RECALL_TICKET: 'recall-ticket',
  REQUEUE_MISSED: 'requeue-missed',
  FINISH_MISSED: 'finish-missed',
  SELECT_COUNTER: 'select-counter',
  ADD_COUNTER: 'add-counter',
  UPDATE_COUNTER: 'update-counter',
  TOGGLE_COUNTER: 'toggle-counter',
  QUEUE_STATE_UPDATED: 'queue-state-updated',
  CONNECTION_STATUS_CHANGED: 'connection-status-changed',
  SHOW_TOAST: 'show-toast',
  OPERATION_RESULT: 'operation-result',
} as const;
