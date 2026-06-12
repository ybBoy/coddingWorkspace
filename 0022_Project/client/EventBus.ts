type Listener = (...args: any[]) => void;

class EventBusClass {
  private listeners: Map<string, Listener[]> = new Map();

  on(event: string, fn: Listener) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event)!.push(fn);
    return () => {
      const arr = this.listeners.get(event);
      if (arr) {
        const idx = arr.indexOf(fn);
        if (idx > -1) arr.splice(idx, 1);
      }
    };
  }

  emit(event: string, ...args: any[]) {
    const arr = this.listeners.get(event);
    if (arr) {
      arr.slice().forEach(fn => fn(...args));
    }
  }
}

const EventBus = new EventBusClass();
export default EventBus;
