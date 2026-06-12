/**
 * 共享类型定义
 * 与后端 Order.java / DishItem.java 字段保持一致
 */

export type OrderStatus = 'NEW' | 'COOKING' | 'DONE'

export interface DishItem {
  id: string
  name: string
  quantity: number
  note?: string
  redo?: boolean
}

export interface Order {
  id: string
  tableNo: string
  dishes: DishItem[]
  remark?: string
  status: OrderStatus
  createdAt: number
  updatedAt: number
}

/** 后端 -> 前端 WebSocket 消息包装 */
export interface WsMessage {
  type: 'ORDERS' | 'PONG'
  data?: Order[]
}

/** 前端 -> 后端 WebSocket 消息类型 */
export type WsAction =
  | { type: 'CREATE'; tableNo: string; dishes: DishItem[]; remark?: string }
  | { type: 'START'; orderId: string }
  | { type: 'FINISH'; orderId: string }
  | { type: 'REDO'; orderId: string; dishId: string }
  | { type: 'UNREDO'; orderId: string; dishId: string }
  | { type: 'PING' }
