import type { Order } from './types'
import OrderDetail from './OrderDetail'

/**
 * 单独一列（新订单 / 制作中 / 已出餐）
 *
 * 职责：
 *   - 显示列标题 + 订单数量 Badge
 *   - 遍历 orders 渲染一组 OrderDetail 卡片
 *   - 如果没有订单，显示 "暂无..." 空状态提示
 *
 * 被 KitchenBoard 调用 3 次（一次一列）
 */
interface Props {
  colClass: 'new' | 'cooking' | 'done'
  title: string
  orders: Order[]
  totalCount: number  // 过滤前的总数（用于 Badge 显示真实积压量）
}

export default function OrderColumn({ colClass, title, orders, totalCount }: Props) {
  return (
    <section className={`column ${colClass}`}>
      <div className="column-header">
        <h2>{title}</h2>
        <span className="count-badge">{totalCount}</span>
      </div>

      {orders.length === 0 ? (
        <div className="empty-tip">
          {colClass === 'done' ? '📋 暂时没有已出餐订单' : '🎉 暂无订单，休息一下'}
        </div>
      ) : (
        orders.map((o) => <OrderDetail key={o.id} order={o} column={colClass} />)
      )}
    </section>
  )
}
