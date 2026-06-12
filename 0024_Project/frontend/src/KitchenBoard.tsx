import { useEffect, useState } from 'react'
import type { Order, OrderStatus } from './types'
import OrderColumn from './OrderColumn'
import { EventBus, EVT } from './EventBus'

/**
 * 后厨看板三列布局
 *
 * 职责：
 *   - 从父组件 App 拿到全量订单（App 通过 EventBus.ORDERS_UPDATED 订阅并 props 传入
 *   - 从 EventBus 订阅搜索关键字（SEARCH_CHANGED）
 *   - 按状态把订单拆分成 3 列：新订单 / 制作中 / 已出餐
 *   - 每列内部再按时间正序（最早的在上，保证先到先做）
 *
 *   新订单列（NEW）：每个订单只显示 "开始制作" 按钮
 *   制作中列（COOKING）："完成出餐" 按钮 + 每个菜品可单独 "标记重做"
 *   已出餐列（DONE）：绿色卡片，不显示操作按钮
 */
type ColDef = {
  key: OrderStatus
  title: string
  cls: 'new' | 'cooking' | 'done'
  icon: string
}

const COLS: ColDef[] = [
  { key: 'NEW',     title: '🆕 新订单',   cls: 'new',     icon: '🆕' },
  { key: 'COOKING', title: '🔥 制作中',   cls: 'cooking', icon: '🔥' },
  { key: 'DONE',    title: '✅ 已出餐',   cls: 'done',    icon: '✅' },
]

export default function KitchenBoard({ allOrders }: { allOrders: Order[] }) {
  const [keyword, setKeyword] = useState('')

  useEffect(() => {
    const unSub = EventBus.on<string>(EVT.SEARCH_CHANGED, (k) => setKeyword(k ?? ''))
    return unSub
  }, [])

  // --- 按桌号 / 订单号过滤
  const kw = keyword.toLowerCase()
  const filtered = kw
    ? allOrders.filter((o) =>
        o.id.toLowerCase().includes(kw) ||
        (o.tableNo ?? '').toLowerCase().includes(kw),
      )
    : allOrders

  const byStatus = COLS.map((col) => {
    const list = filtered
      .filter((o) => o.status === col.key)
      .sort((a, b) => a.createdAt - b.createdAt)
    return { col, list }
  })

  return (
    <div className="kitchen-board">
      {byStatus.map(({ col, list }) => (
        <OrderColumn
          key={col.key}
          colClass={col.cls}
          title={col.title}
          orders={list}
          totalCount={allOrders.filter((o) => o.status === col.key).length}
        />
      ))}
    </div>
  )
}
