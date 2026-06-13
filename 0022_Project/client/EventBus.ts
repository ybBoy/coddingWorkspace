type Listener = (...args: any[]) => void;

class EventBusClass {
  private listeners: Map<string, Listener[]> = new Map();
  private stickyCache: Map<string, any[]> = new Map();

  on(event: string, fn: Listener, replaySticky = true) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event)!.push(fn);
    if (replaySticky && this.stickyCache.has(event)) {
      fn(...this.stickyCache.get(event)!);
    }
    return () => {
      const arr = this.listeners.get(event);
      if (arr) {
        const idx = arr.indexOf(fn);
        if (idx > -1) arr.splice(idx, 1);
      }
    };
  }

  emit(event: string, ...args: any[]) {
    this.stickyCache.set(event, args);
    const arr = this.listeners.get(event);
    if (arr) {
      arr.slice().forEach(fn => fn(...args));
    }
  }
}

const EventBus = new EventBusClass();
export default EventBus;
