type Listener<T = any> = (data: T) => void;

class EventBus {
  private listeners: Map<string, Set<Listener>> = new Map();

  on<T = any>(event: string, listener: Listener<T>): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(listener);
    return () => this.off(event, listener);
  }

  off<T = any>(event: string, listener: Listener<T>): void {
    const set = this.listeners.get(event);
    if (set) {
      set.delete(listener);
    }
  }

  emit<T = any>(event: string, data?: T): void {
    const set = this.listeners.get(event);
    if (set) {
      set.forEach((fn) => {
        try {
          fn(data);
        } catch (e) {
          console.error(`EventBus error on "${event}":`, e);
        }
      });
    }
  }

  once<T = any>(event: string, listener: Listener<T>): () => void {
    const wrapped: Listener<T> = (data) => {
      this.off(event, wrapped);
      listener(data);
    };
    return this.on(event, wrapped);
  }
}

export const eventBus = new EventBus();

export type SeatEventMap = {
  'seat:sit': { seatId: number; nickname: string };
  'seat:away': { seatId: number; nickname: string };
  'seat:leave': { seatId: number; nickname: string };
  'seat:forceRelease': { seatId: number };
  'state:init': { seats: SeatData[]; actions: SeatActionData[] };
  'state:update': { seats: SeatData[]; actions: SeatActionData[] };
  'ws:connected': void;
  'ws:disconnected': void;
  'ws:error': string;
};

export interface SeatData {
  id: number;
  row: number;
  col: number;
  status: 'free' | 'occupied' | 'away' | 'releasable';
  nickname: string | null;
  awaySince: number;
}

export interface SeatActionData {
  seatId: number;
  action: string;
  nickname: string;
  timestamp: number;
}
