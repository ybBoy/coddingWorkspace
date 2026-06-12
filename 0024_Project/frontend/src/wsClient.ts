import type { Order, WsAction } from './types'
import { EventBus, EVT } from './EventBus'

/**
 * WebSocket 客户端封装
 *
 * 职责：
 *   - 建立/维护与后端 ws://.../ws 的长连接（带自动重连）
 *   - 收消息：解析 JSON，把 ORDERS 类型的消息通过 EventBus 广播给各组件
 *   - 发消息：send(action) 供组件调用（新增订单、状态流转、重做等）
 *
 * 组件调用关系：
 *   App.tsx           -> wsClient.start()  启动连接
 *   OrderEntry.tsx    -> wsClient.send({ type:'CREATE', ... })  下单
 *   KitchenBoard/...  -> wsClient.send({ type:'START'/'FINISH'/'REDO', ... })
 *   所有组件          -> EventBus.on(EVT.ORDERS_UPDATED, ...) 拿最新订单列表
 */
class WsClient {
  private ws: WebSocket | null = null
  private url: string
  private reconnectTimer: any = null
  private reconnectDelay = 2000
  private pingTimer: any = null

  constructor(path: string = '/ws') {
    // 根据当前页面的协议/主机拼出 ws 地址（开发时 Vite proxy 会转到 8080）
    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
    this.url = `${proto}//${location.host}${path}`
  }

  start() {
    this.connect()
  }

  private connect() {
    EventBus.emit(EVT.WS_STATUS, 'connecting')
    try {
      this.ws = new WebSocket(this.url)
    } catch (e) {
      console.error('[WS] create failed', e)
      this.scheduleReconnect()
      return
    }

    this.ws.onopen = () => {
      EventBus.emit(EVT.WS_STATUS, 'open')
      this.reconnectDelay = 2000
      // 心跳：每 25 秒发一次 PING（要小于后端心跳 30 秒，防止意外断开）
      clearInterval(this.pingTimer)
      this.pingTimer = setInterval(() => this.send({ type: 'PING' }), 25000)
    }

    this.ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data)
        if (msg.type === 'ORDERS' && Array.isArray(msg.data)) {
          const orders = msg.data as Order[]
          EventBus.emit(EVT.ORDERS_UPDATED, orders)
        }
      } catch (e) {
        console.error('[WS] parse failed', e)
      }
    }

    this.ws.onclose = () => {
      EventBus.emit(EVT.WS_STATUS, 'closed')
      clearInterval(this.pingTimer)
      this.scheduleReconnect()
    }

    this.ws.onerror = (e) => {
      console.error('[WS] error', e)
    }
  }

  private scheduleReconnect() {
    clearTimeout(this.reconnectTimer)
    this.reconnectTimer = setTimeout(() => {
      // 指数退避，最大 10 秒
      this.reconnectDelay = Math.min(this.reconnectDelay * 1.5, 10000)
      this.connect()
    }, this.reconnectDelay)
  }

  /** 发送一条后端指令（CREATE/START/FINISH/REDO/UNREDO/PING） */
  send(action: WsAction) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('[WS] not connected, drop msg', action)
      return
    }
    this.ws.send(JSON.stringify(action))
  }

  close() {
    clearInterval(this.pingTimer)
    clearTimeout(this.reconnectTimer)
    if (this.ws) this.ws.close()
  }
}

/** 单例导出 */
export const wsClient = new WsClient()
