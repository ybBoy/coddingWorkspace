/**
 * 轻量级 EventBus（发布/订阅）
 *
 * 为什么不用 Redux / Context？
 *   需求明确："不要用全局变量，组件之间通过一个简单的 EventBus 传递事件"
 *   这个 EventBus 的职责非常单一：
 *     - App 收到 WebSocket 推送后 publish('orders:updated', orders)
 *     - OrderEntry / KitchenBoard / OrderColumn 等组件订阅这个事件并刷新自己
 *     - 其他 UI 事件（如切换视图、搜索）也可通过 EventBus 广播
 *
 * 用法：
 *   const unsub = EventBus.on('orders:updated', (orders) => { ... })
 *   EventBus.emit('orders:updated', orders)
 *   unsub()   // 取消订阅
 */

type Handler<T = any> = (payload: T) => void

class EventBusImpl {
  private map = new Map<string, Set<Handler>>()

  /** 订阅事件，返回取消订阅的函数（组件卸载时调用） */
  on<T = any>(event: string, handler: Handler<T>): () => void {
    if (!this.map.has(event)) this.map.set(event, new Set())
    this.map.get(event)!.add(handler as Handler)
    return () => this.off(event, handler as Handler)
  }

  /** 主动取消订阅（一般直接用 on 返回的函数更方便） */
  off(event: string, handler: Handler): void {
    const s = this.map.get(event)
    if (s) s.delete(handler)
  }

  /** 发布事件，触发所有订阅者 */
  emit<T = any>(event: string, payload?: T): void {
    const s = this.map.get(event)
    if (!s) return
    // 拷贝一份再遍历，避免某个 handler 内部 off/on 导致迭代异常
    Array.from(s).forEach((fn) => {
      try { fn(payload) } catch (e) { console.error(e) }
    })
  }
}

/** 单例导出，整个前端共享这一个 EventBus */
export const EventBus = new EventBusImpl()

/** 约定的事件名字符串常量，避免写散 */
export const EVT = {
  ORDERS_UPDATED: 'orders:updated',         // 载荷: Order[]   WebSocket 推送新订单数据时触发
  MENU_UPDATED: 'menu:updated',             // 载荷: MenuItem[] 菜单数据更新
  VIEW_CHANGED: 'view:changed',             // 载荷: 'entry' | 'board' | 'history'
  SEARCH_CHANGED: 'search:changed',         // 载荷: string   搜索框输入时触发
  WS_STATUS: 'ws:status',                   // 载荷: 'connecting' | 'open' | 'closed'
  NEW_ORDER_ARRIVED: 'order:new',           // 载荷: Order      新订单到达
  BIGSCREEN_TOGGLE: 'bigscreen:toggle',     // 载荷: boolean   大屏模式开关
  STATION_CHANGED: 'station:changed',       // 载荷: string | null  工位切换（null = 全部）
  HISTORY_RESULT: 'history:result',         // 载荷: {date, orders}
  DISH_ANALYSIS_RESULT: 'analysis:result',  // 载荷: {date, analysis}
  EXPORT_CSV_RESULT: 'export:csv',          // 载荷: {date, csv}
  HISTORY_DATES: 'history:dates',           // 载荷: string[]
  ROLE_CHANGED: 'role:changed',             // 载荷: Role
  EDIT_ORDER: 'order:edit',                 // 载荷: Order  打开改单表单
} as const
