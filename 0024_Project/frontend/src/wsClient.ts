import type { Order, MenuItem, WsAction } from './types'
import { EventBus, EVT } from './EventBus'

/**
 * WebSocket 客户端封装
 *
 * 职责：
 *   - 建立/维护与后端 ws://.../ws 的长连接（带自动重连）
 *   - 收消息：解析 JSON，把 ORDERS / MENU 类型的消息通过 EventBus 广播
 *   - 发消息：send(action) 供组件调用
 *   - 离线队列：连接断开时，待发送消息暂存于 pendingQueue，重连成功后批量补发
 *   - 新订单检测：对比新旧订单列表，检测出新到的订单并触发 NEW_ORDER_ARRIVED 事件（供声音/桌面通知使用）
 *
 * 组件调用关系：
 *   App.tsx           -> wsClient.start()  启动连接
 *   OrderEntry.tsx    -> wsClient.send({ type:'CREATE', ... })  下单
 *   KitchenBoard/...  -> wsClient.send({ type:'START'/'FINISH'/'REDO'/... })
 *   所有组件          -> EventBus.on(...) 拿最新数据
 */
class WsClient {
  private ws: WebSocket | null = null
  private url: string
  private reconnectTimer: any = null
  private reconnectDelay = 2000
  private pingTimer: any = null

  private prevOrderIds: Set<string> = new Set()   // 上一次收到的订单ID集合，用于检测新订单
  private pendingQueue: WsAction[] = []           // 离线期间待发送的消息队列

  constructor(path: string = '/ws') {
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
      clearInterval(this.pingTimer)
      this.pingTimer = setInterval(() => this.sendInternal({ type: 'PING' }), 25000)
      // 重连成功后把离线队列的消息依次发出去
      this.flushPendingQueue()
    }

    this.ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data)
        if (msg.type === 'ORDERS' && Array.isArray(msg.data)) {
          const orders = msg.data as Order[]
          this.detectNewOrders(orders)
          this.prevOrderIds = new Set(orders.map((o) => o.id))
          EventBus.emit(EVT.ORDERS_UPDATED, orders)
        } else if (msg.type === 'MENU' && Array.isArray(msg.data)) {
          EventBus.emit(EVT.MENU_UPDATED, msg.data as MenuItem[])
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

  /** 对比新旧订单，找出本次新到的订单，逐个触发 NEW_ORDER_ARRIVED 事件 */
  private detectNewOrders(orders: Order[]) {
    for (const o of orders) {
      if (!this.prevOrderIds.has(o.id)) {
        // 只有 NEW 状态的才算"新订单"（恢复历史的已出餐/制作中订单不提醒
        if (o.status === 'NEW') {
          EventBus.emit(EVT.NEW_ORDER_ARRIVED, o)
        }
      }
    }
  }

  private scheduleReconnect() {
    clearTimeout(this.reconnectTimer)
    this.reconnectTimer = setTimeout(() => {
      this.reconnectDelay = Math.min(this.reconnectDelay * 1.5, 10000)
      this.connect()
    }, this.reconnectDelay)
  }

  /** 重连成功后把缓存的消息全部发出去 */
  private flushPendingQueue() {
    if (this.pendingQueue.length === 0) return
    console.log(`[WS] flush pending queue: ${this.pendingQueue.length} 条`)
    const items = [...this.pendingQueue]
    this.pendingQueue = []
    items.forEach((a) => this.sendInternal(a))
  }

  /** 真正执行发送（仅内部使用，跳过队列判断） */
  private sendInternal(action: WsAction) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return
    this.ws.send(JSON.stringify(action))
  }

  /** 对外发送接口：连在线就直接发，离线则入队 */
  send(action: WsAction) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      // PING 消息不进队列，其他都入队
      if (action.type !== 'PING') {
        console.warn('[WS] offline, enqueue msg', action.type)
        this.pendingQueue.push(action)
      }
      return
    }
    this.sendInternal(action)
  }

  /** 当前离线队列大小（用于 UI 提示"有 N 条待发送"） */
  get pendingCount() { return this.pendingQueue.length }

  close() {
    clearInterval(this.pingTimer)
    clearTimeout(this.reconnectTimer)
    if (this.ws) this.ws.close()
  }
}

/** 单例导出 */
export const wsClient = new WsClient()
