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

export type SeatStatus = 'free' | 'occupied' | 'away' | 'releasable';
export type ZoneType = 'window' | 'computer' | 'quiet' | 'standard';

export type SeatEventMap = {
  'seat:sit': { seatId: number; nickname: string };
  'seat:away': { seatId: number; nickname: string };
  'seat:leave': { seatId: number; nickname: string };
  'seat:forceRelease': { seatId: number; isAdmin?: boolean };
  'seat:select': { seatId: number | null };
  'state:init': { seats: SeatData[]; actions: SeatActionData[]; broadcast: string; broadcastTimestamp: number };
  'state:update': { seats: SeatData[]; actions: SeatActionData[] };
  'ws:connected': void;
  'ws:disconnected': void;
  'ws:error': string;
  'broadcast:update': { message: string; timestamp: number };
  'admin:loginResult': { success: boolean };
  'admin:stats': StatsData;
  'admin:export': SeatActionData[];
  'toast:show': { message: string; type?: 'info' | 'success' | 'warning' | 'error'; duration?: number };
};

export interface SeatData {
  id: number;
  row: number;
  col: number;
  status: SeatStatus;
  nickname: string | null;
  awaySince: number;
  zone: ZoneType;
}

export interface SeatActionData {
  seatId: number;
  action: string;
  nickname: string;
  timestamp: number;
}

export interface StatsData {
  total: number;
  free: number;
  occupied: number;
  away: number;
  releasable: number;
  occupancyRate: number;
  todayForceReleases: number;
  todayUniqueUsers: number;
  zoneStats: Record<string, number[]>;
  hourlyDistribution: number[];
}

export const ZONE_LABELS: Record<ZoneType, string> = {
  window: '靠窗区',
  computer: '电脑区',
  quiet: '安静区',
  standard: '普通区',
};
