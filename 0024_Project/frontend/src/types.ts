/**
 * 共享类型定义
 * 与后端 Order.java / DishItem.java / MenuItem.java 字段保持一致
 */

export type OrderStatus = 'NEW' | 'COOKING' | 'DONE'
export type Priority = 'NORMAL' | 'HIGH'

export interface DishItem {
  id: string
  name: string
  quantity: number
  note?: string
  redo?: boolean
  done?: boolean     // 菜品级完成状态
  finishedAt?: number // 菜品完成时间戳
}

export interface Order {
  id: string
  tableNo: string
  dishes: DishItem[]
  remark?: string
  status: OrderStatus
  priority: Priority  // 普通 / 加急
  createdAt: number
  updatedAt: number
  finishedAt?: number // 整单完成时间戳
}

/** 菜单条目（常用菜品） */
export interface MenuItem {
  id: string
  name: string
  category?: string
  price?: number
  sort?: number
  enabled?: boolean
}

/** 后端 -> 前端 WebSocket 消息包装 */
export interface WsMessage {
  type: 'ORDERS' | 'MENU' | 'PONG'
  data?: Order[] | MenuItem[]
}

/** 前端 -> 后端 WebSocket 消息类型 */
export type WsAction =
  | { type: 'CREATE'; tableNo: string; dishes: DishItem[]; remark?: string; urgent?: boolean }
  | { type: 'START'; orderId: string }
  | { type: 'FINISH'; orderId: string }
  | { type: 'DISH_DONE'; orderId: string; dishId: string; done: boolean }
  | { type: 'REDO'; orderId: string; dishId: string }
  | { type: 'UNREDO'; orderId: string; dishId: string }
  | { type: 'SET_URGENT'; orderId: string; urgent: boolean }
  | { type: 'MENU_LIST' }
  | { type: 'MENU_ADD'; item: MenuItem }
  | { type: 'MENU_UPDATE'; item: MenuItem }
  | { type: 'MENU_DELETE'; id: string }
  | { type: 'PING' }
