type EventCallback = (...args: any[]) => void;

class EventBus {
  private events: Map<string, EventCallback[]> = new Map();

  on(event: string, callback: EventCallback): void {
    if (!this.events.has(event)) {
      this.events.set(event, []);
    }
    this.events.get(event)!.push(callback);
  }

  off(event: string, callback: EventCallback): void {
    const callbacks = this.events.get(event);
    if (!callbacks) return;
    const index = callbacks.indexOf(callback);
    if (index > -1) {
      callbacks.splice(index, 1);
    }
  }

  emit(event: string, ...args: any[]): void {
    const callbacks = this.events.get(event);
    if (!callbacks) return;
    callbacks.forEach((cb) => {
      try {
        cb(...args);
      } catch (e) {
        console.error(`[EventBus] Error in callback for event "${event}":`, e);
      }
    });
  }

  clear(): void {
    this.events.clear();
  }
}

export const eventBus = new EventBus();

export const Events = {
  ROOM_SELECTED: 'room:selected',
  CHECK_IN: 'action:checkIn',
  CHECK_OUT: 'action:checkOut',
  CLEAN: 'action:clean',
  MAINTENANCE: 'action:maintenance',
  REPAIR_DONE: 'action:repairDone',
  FILTER_CHANGED: 'filter:changed',
  WS_CONNECTED: 'ws:connected',
  WS_DISCONNECTED: 'ws:disconnected',
};
