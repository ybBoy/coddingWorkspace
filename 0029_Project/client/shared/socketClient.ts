import { LedgerState, OutgoingMessage, ConnectionStatus, ExportResult } from './types'
import { eventBus } from './EventBus'

const QUEUE_KEY = 'ledger_offline_queue'

const WRITE_TYPES = new Set([
  'ADD_EXPENSE', 'EDIT_EXPENSE', 'DELETE_EXPENSE',
  'SET_BUDGET', 'REMOVE_BUDGET', 'UPDATE_CONFIG'
])

class SocketClient {
  private ws: WebSocket | null = null
  private reconnectTimer: number | null = null
  private status: ConnectionStatus = 'disconnected'
  private url: string
  private queue: OutgoingMessage[] = []

  constructor() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    this.url = `${protocol}//${host}/ledger`
    this.loadQueue()
  }

  connect(): void {
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    this.setStatus('connecting')
    this.ws = new WebSocket(this.url)

    this.ws.onopen = () => {
      this.setStatus('connected')
      this.cancelReconnect()
      this.flushQueue()
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'EXPORT_RESULT') {
          eventBus.emit('export:result', data.payload as ExportResult)
        } else {
          eventBus.emit('state:updated', data as LedgerState)
        }
      } catch (e) {
        console.error('Failed to parse server message:', e)
      }
    }

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error)
    }

    this.ws.onclose = () => {
      this.setStatus('disconnected')
      this.scheduleReconnect()
    }
  }

  disconnect(): void {
    this.cancelReconnect()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  send(message: OutgoingMessage): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    } else if (WRITE_TYPES.has(message.type)) {
      this.queue.push(message)
      this.saveQueue()
      const count = this.queue.length
      eventBus.emit('toast:show', {
        message: count === 1 ? '网络离线，已暂存 1 条操作' : `网络离线，已暂存 ${count} 条操作`,
        type: 'warning'
      })
    } else {
      eventBus.emit('toast:show', {
        message: '网络离线，请稍后重试',
        type: 'warning'
      })
    }
  }

  getStatus(): ConnectionStatus {
    return this.status
  }

  getPendingCount(): number {
    return this.queue.length
  }

  private flushQueue(): void {
    if (this.queue.length === 0) return
    const count = this.queue.length
    let sent = 0
    while (this.queue.length > 0 && this.ws && this.ws.readyState === WebSocket.OPEN) {
      const msg = this.queue.shift()!
      this.ws.send(JSON.stringify(msg))
      sent++
    }
    this.saveQueue()
    if (sent > 0) {
      eventBus.emit('toast:show', {
        message: `网络恢复，已同步 ${sent} 条暂存操作`,
        type: 'success'
      })
    }
  }

  private loadQueue(): void {
    try {
      const raw = localStorage.getItem(QUEUE_KEY)
      if (raw) {
        this.queue = JSON.parse(raw) as OutgoingMessage[]
      }
    } catch (e) {
      console.error('Failed to load offline queue:', e)
      this.queue = []
    }
  }

  private saveQueue(): void {
    try {
      localStorage.setItem(QUEUE_KEY, JSON.stringify(this.queue))
    } catch (e) {
      console.error('Failed to save offline queue:', e)
    }
  }

  private setStatus(status: ConnectionStatus): void {
    if (this.status !== status) {
      this.status = status
      eventBus.emit('connection:changed', status)
    }
  }

  private scheduleReconnect(): void {
    this.cancelReconnect()
    this.reconnectTimer = window.setTimeout(() => {
      console.log('Reconnecting to WebSocket...')
      this.connect()
    }, 3000)
  }

  private cancelReconnect(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }
}

export const socketClient = new SocketClient()
