import type { EventMap } from './types';

type HandlerFn<K extends keyof EventMap> = (data: EventMap[K]) => void;

class EventBus {
  private listeners: Map<keyof EventMap, Set<HandlerFn<any>>> = new Map();

  on<K extends keyof EventMap>(event: K, handler: HandlerFn<K>): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(handler);
    return () => this.off(event, handler);
  }

  off<K extends keyof EventMap>(event: K, handler: HandlerFn<K>): void {
    const set = this.listeners.get(event);
    if (set) set.delete(handler);
  }

  emit<K extends keyof EventMap>(event: K, data: EventMap[K]): void {
    const set = this.listeners.get(event);
    if (set) {
      set.forEach(fn => {
        try { fn(data); } catch (e) { console.error(`[EventBus:${event}]`, e); }
      });
    }
  }

  clear(): void {
    this.listeners.clear();
  }
}

export const eventBus = new EventBus();
export default eventBus;
