type EventCallback = (data?: any) => void;

class EventBus {
  private events: Record<string, EventCallback[]> = {};

  on(event: string, callback: EventCallback): void {
    if (!this.events[event]) {
      this.events[event] = [];
    }
    this.events[event].push(callback);
  }

  off(event: string, callback: EventCallback): void {
    if (!this.events[event]) {
      return;
    }
    const index = this.events[event].indexOf(callback);
    if (index > -1) {
      this.events[event].splice(index, 1);
    }
  }

  emit(event: string, data?: any): void {
    if (!this.events[event]) {
      return;
    }
    this.events[event].forEach((callback) => {
      try {
        callback(data);
      } catch (e) {
        console.error('EventBus error:', e);
      }
    });
  }

  clear(): void {
    this.events = {};
  }
}

export const eventBus = new EventBus();
export default eventBus;
