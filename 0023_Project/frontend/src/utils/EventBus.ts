import { EventType } from '../types';

type Listener = (data?: any) => void;

class EventBus {
  private listeners: Map<EventType, Set<Listener>> = new Map();

  on(event: EventType, listener: Listener): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(listener);
    return () => this.off(event, listener);
  }

  off(event: EventType, listener: Listener): void {
    const set = this.listeners.get(event);
    if (set) {
      set.delete(listener);
    }
  }

  emit(event: EventType, data?: any): void {
    const set = this.listeners.get(event);
    if (set) {
      set.forEach(listener => {
        try {
          listener(data);
        } catch (e) {
          console.error('EventBus listener error:', e);
        }
      });
    }
  }
}

export const eventBus = new EventBus();
