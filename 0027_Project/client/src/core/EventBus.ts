export type EventCallback = (data?: any) => void

export const EVENT_CHECKIN = 'checkIn'
export const EVENT_CHECKIN_ACK = 'checkInAck'
export const EVENT_CHECKIN_ERROR = 'checkInError'
export const EVENT_FILTER_CHANGE = 'filterChange'
export const EVENT_STATS_REFRESH = 'statsRefresh'
export const EVENT_RECORDS_UPDATE = 'recordsUpdate'
export const EVENT_RANGE_STATS = 'rangeStats'
export const EVENT_PEAK_ALERT = 'peakAlert'

class EventBus {
  private listeners: Map<string, Set<EventCallback>> = new Map()

  on(event: string, cb: EventCallback): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set())
    }
    this.listeners.get(event)!.add(cb)
    return () => {
      this.off(event, cb)
    }
  }

  off(event: string, cb: EventCallback): void {
    const set = this.listeners.get(event)
    if (set) {
      set.delete(cb)
    }
  }

  emit(event: string, data?: any): void {
    const set = this.listeners.get(event)
    if (set) {
      set.forEach((cb) => {
        try {
          cb(data)
        } catch (e) {
          console.error(`[EventBus] Error in listener for ${event}:`, e)
        }
      })
    }
  }
}

export const eventBus = new EventBus()
