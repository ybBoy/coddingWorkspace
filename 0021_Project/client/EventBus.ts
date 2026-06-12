// EventBus 事件总线
// 职责：提供组件间的事件发布/订阅机制，避免使用全局变量
// 使用方式：组件通过 eventBus.on() 订阅事件，通过 eventBus.emit() 发布事件
// 数据流：用户操作 → 组件 emit 事件 → App 组件订阅事件 → 通过 WebSocket 发送给后端

type EventCallback = (...args: any[]) => void;

class EventBus {
  private events: Map<string, EventCallback[]> = new Map();

  // 订阅事件
  on(event: string, callback: EventCallback): void {
    if (!this.events.has(event)) {
      this.events.set(event, []);
    }
    this.events.get(event)!.push(callback);
  }

  // 取消订阅
  off(event: string, callback: EventCallback): void {
    const callbacks = this.events.get(event);
    if (!callbacks) return;
    const index = callbacks.indexOf(callback);
    if (index > -1) {
      callbacks.splice(index, 1);
    }
  }

  // 发布事件
  emit(event: string, ...args: any[]): void {
    const callbacks = this.events.get(event);
    if (!callbacks) return;
    callbacks.forEach((cb) => {
      try {
        cb(...args);
      } catch (e) {
        console.error(`EventBus error in event [${event}]:`, e);
      }
    });
  }

  // 只订阅一次
  once(event: string, callback: EventCallback): void {
    const wrapper = (...args: any[]) => {
      callback(...args);
      this.off(event, wrapper);
    };
    this.on(event, wrapper);
  }
}

// 全局单例
export const eventBus = new EventBus();

// 事件类型常量
export const EVENTS = {
  // 用户发起预约
  BOOKING_REQUEST: 'booking:request',
  // 用户取消预约
  CANCEL_REQUEST: 'cancel:request',
  // 工作人员签到
  CHECKIN_REQUEST: 'checkin:request',
  // 场次选择变化
  SESSION_SELECTED: 'session:selected',
  // 收到服务端场次更新
  SESSIONS_UPDATED: 'sessions:updated',
  // 收到活动动态
  ACTIVITY_RECEIVED: 'activity:received',
  // 连接状态变化
  CONNECTION_CHANGED: 'connection:changed',
};

export default eventBus;
