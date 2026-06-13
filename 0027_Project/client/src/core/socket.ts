import {
  eventBus,
  EVENT_CHECKIN,
  EVENT_CHECKIN_ACK,
  EVENT_CHECKIN_ERROR,
  EVENT_STATS_REFRESH,
  EVENT_RECORDS_UPDATE,
  EVENT_RANGE_STATS,
  EVENT_EXPORT_RECORDS,
} from './EventBus'

export interface Booth {
  id: string
  name: string
  description: string
}

export interface Visitor {
  name: string
  phoneSuffix: string
}

export interface CheckInRecord {
  id: string
  boothId: string
  visitor: Visitor
  interestedProjects: string[]
  timestamp: number
}

export interface SnapshotData {
  booths: Booth[]
  records: CheckInRecord[]
  boothStats: Record<string, number>
  projectStats: Record<string, number>
  todayBoothStats: Record<string, number>
  todayProjectStats: Record<string, number>
  todayTotal: number
  peakBooths: string[]
  availableProjects: string[]
}

export interface RangeStatsData {
  records: CheckInRecord[]
  boothStats: Record<string, number>
  projectStats: Record<string, number>
  range: string
}

type WSMessageType =
  | 'init'
  | 'checkIn'
  | 'checkInAck'
  | 'statsUpdate'
  | 'rangeStats'
  | 'exportRecords'
  | 'error'

interface WSMessage {
  type: WSMessageType
  payload: any
}

const RECONNECT_INTERVAL = 2000
const MAX_RECONNECT_COUNT = 10

class WSocket {
  private static instance: WSocket
  private ws: WebSocket | null = null
  private reconnectTimer: any = null
  private reconnectCount: number = 0
  private url: string = 'ws://localhost:8887'
  private requestCounter: number = 0

  private constructor() {}

  static getInstance(): WSocket {
    if (!WSocket.instance) {
      WSocket.instance = new WSocket()
    }
    return WSocket.instance
  }

  connect(url: string = 'ws://localhost:8887'): void {
    this.url = url

    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      return
    }

    try {
      this.ws = new WebSocket(url)

      this.ws.onopen = () => {
        console.log('[WSocket] Connected')
        this.reconnectCount = 0
        this.clearReconnectTimer()
      }

      this.ws.onmessage = (event) => {
        try {
          const msg: WSMessage = JSON.parse(event.data)
          this.handleMessage(msg)
        } catch (e) {
          console.error('[WSocket] Parse message error:', e)
        }
      }

      this.ws.onerror = (error) => {
        console.error('[WSocket] Error:', error)
      }

      this.ws.onclose = () => {
        console.log('[WSocket] Disconnected')
        this.scheduleReconnect()
      }
    } catch (e) {
      console.error('[WSocket] Connect error:', e)
      this.scheduleReconnect()
    }
  }

  disconnect(): void {
    this.clearReconnectTimer()
    this.reconnectCount = MAX_RECONNECT_COUNT
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  private handleMessage(msg: WSMessage): void {
    const { type, payload } = msg

    switch (type) {
      case 'init':
        eventBus.emit(EVENT_STATS_REFRESH, payload)
        eventBus.emit(EVENT_RECORDS_UPDATE, payload.records)
        break
      case 'checkIn':
        eventBus.emit(EVENT_CHECKIN, payload.record)
        eventBus.emit(EVENT_STATS_REFRESH, payload)
        eventBus.emit(EVENT_RECORDS_UPDATE, payload.recentRecords)
        break
      case 'checkInAck':
        eventBus.emit(EVENT_CHECKIN_ACK, payload)
        break
      case 'statsUpdate':
        eventBus.emit(EVENT_STATS_REFRESH, payload)
        break
      case 'rangeStats':
        eventBus.emit(EVENT_RANGE_STATS, payload)
        break
      case 'exportRecords':
        eventBus.emit(EVENT_EXPORT_RECORDS, payload.records)
        break
      case 'error':
        eventBus.emit(EVENT_CHECKIN_ERROR, payload)
        console.error('[WSocket] Server error:', payload)
        break
      default:
        console.warn('[WSocket] Unknown message type:', type)
    }
  }

  private genRequestId(): string {
    this.requestCounter++
    return `req_${Date.now()}_${this.requestCounter}`
  }

  private scheduleReconnect(): void {
    if (this.reconnectCount >= MAX_RECONNECT_COUNT) {
      console.warn('[WSocket] Max reconnect attempts reached')
      return
    }

    this.clearReconnectTimer()
    this.reconnectCount++
    console.log(
      `[WSocket] Reconnecting... attempt ${this.reconnectCount}/${MAX_RECONNECT_COUNT}`
    )

    this.reconnectTimer = setTimeout(() => {
      this.connect(this.url)
    }, RECONNECT_INTERVAL)
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  sendCheckIn(data: {
    boothId: string
    visitor: Visitor
    interestedProjects: string[]
  }): string {
    const requestId = this.genRequestId()

    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('[WSocket] Cannot send: not connected')
      return requestId
    }

    this.ws.send(
      JSON.stringify({
        type: 'checkIn',
        payload: {
          requestId,
          ...data,
        },
      })
    )

    return requestId
  }

  requestStats(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('[WSocket] Cannot send: not connected')
      return
    }

    this.ws.send(
      JSON.stringify({
        type: 'getStats',
      })
    )
  }

  requestRecordsByRange(range: '10min' | 'today' | 'all'): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('[WSocket] Cannot send: not connected')
      return
    }

    this.ws.send(
      JSON.stringify({
        type: 'getRecordsByRange',
        payload: { range },
      })
    )
  }

  requestExportRecords(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('[WSocket] Cannot send: not connected')
      return
    }

    this.ws.send(
      JSON.stringify({
        type: 'exportRecords',
      })
    )
  }
}

export const socket = WSocket.getInstance()
