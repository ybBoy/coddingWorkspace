/**
 * 共享类型定义
 * 与后端 Order.java / DishItem.java / MenuItem.java 字段保持一致
 */

export type OrderStatus = 'NEW' | 'COOKING' | 'DONE' | 'CANCELLED'
export type Priority = 'NORMAL' | 'HIGH'
export type Role = 'OWNER' | 'FRONT' | 'KITCHEN'  // 店主 / 前台 / 后厨

export interface DishItem {
  id: string
  name: string
  quantity: number
  note?: string
  station?: string   // 制作工位：热菜/饮品/主食 等
  redo?: boolean
  done?: boolean
  startedAt?: number  // 菜品开始制作时间
  finishedAt?: number // 菜品完成时间戳
}

export interface Order {
  id: string
  tableNo: string
  dishes: DishItem[]
  remark?: string
  status: OrderStatus
  priority: Priority
  createdAt: number
  updatedAt: number
  finishedAt?: number
  cancelledAt?: number
  cancelReason?: string
}

/** 菜单条目（常用菜品） */
export interface MenuItem {
  id: string
  name: string
  category?: string
  station?: string   // 默认制作工位
  price?: number
  sort?: number
  enabled?: boolean
}

/** 菜品分析数据 */
export interface DishAnalysis {
  name: string
  count: number
  avgMinutes: number
  redoCount: number
  redoRate: number
}

/** 后端 -> 前端 WebSocket 消息包装 */
export interface WsMessage {
  type: 'ORDERS' | 'MENU' | 'PONG' | 'HISTORY_DATES' | 'HISTORY_QUERY' | 'DISH_ANALYSIS' | 'EXPORT_CSV'
  data?: any
  date?: string  // 历史查询类消息的日期
}

/** 前端 -> 后端 WebSocket 消息类型 */
export type WsAction =
  | { type: 'CREATE'; tableNo: string; dishes: DishItem[]; remark?: string; urgent?: boolean }
  | { type: 'UPDATE'; orderId: string; tableNo?: string; dishes?: DishItem[]; remark?: string }
  | { type: 'CANCEL'; orderId: string; reason?: string }
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
  | { type: 'HISTORY_DATES' }
  | { type: 'HISTORY_QUERY'; date: string }
  | { type: 'DISH_ANALYSIS'; date: string }
  | { type: 'EXPORT_CSV'; date: string }
  | { type: 'PING' }
