import { EventType, EventMap, EventHandler } from './types'

class EventBus {
  private listeners: Map<EventType, Set<Function>> = new Map()

  on<T extends EventType>(type: T, handler: EventHandler<T>): () => void {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set())
    }
    this.listeners.get(type)!.add(handler)
    return () => this.off(type, handler)
  }

  off<T extends EventType>(type: T, handler: EventHandler<T>): void {
    const handlers = this.listeners.get(type)
    if (handlers) {
      handlers.delete(handler)
    }
  }

  emit<T extends EventType>(type: T, data: EventMap[T]): void {
    const handlers = this.listeners.get(type)
    if (handlers) {
      handlers.forEach((handler) => {
        try {
          handler(data)
        } catch (e) {
          console.error('EventBus handler error:', e)
        }
      })
    }
  }

  clear(): void {
    this.listeners.clear()
  }
}

export const eventBus = new EventBus()
