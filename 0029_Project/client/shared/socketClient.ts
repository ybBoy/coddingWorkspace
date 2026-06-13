import { LedgerState, OutgoingMessage, ConnectionStatus } from './types'
import { eventBus } from './EventBus'

class SocketClient {
  private ws: WebSocket | null = null
  private reconnectTimer: number | null = null
  private status: ConnectionStatus = 'disconnected'
  private url: string

  constructor() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    this.url = `${protocol}//${host}/ledger`
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
    }

    this.ws.onmessage = (event) => {
      try {
        const state: LedgerState = JSON.parse(event.data)
        eventBus.emit('state:updated', state)
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
    } else {
      console.warn('WebSocket not connected, message queued')
    }
  }

  getStatus(): ConnectionStatus {
    return this.status
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
