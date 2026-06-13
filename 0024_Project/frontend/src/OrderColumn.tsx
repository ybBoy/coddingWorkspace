import type { Order } from './types'
import OrderDetail from './OrderDetail'

/**
 * 单独一列（新订单 / 制作中 / 已出餐）
 *
 * 新增：
 *   - highlightStation：从 KitchenBoard 传过来，让 OrderDetail 里对应工位的菜品高亮、其他变淡
 */
interface Props {
  colClass: 'new' | 'cooking' | 'done'
  title: string
  orders: Order[]
  totalCount: number
  highlightStation?: string | null
}

export default function OrderColumn({ colClass, title, orders, totalCount, highlightStation }: Props) {
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
        orders.map((o) => (
          <OrderDetail
            key={o.id}
            order={o}
            column={o.status === 'CANCELLED' ? 'done' : colClass}
            highlightStation={highlightStation ?? null}
          />
        ))
      )}
    </section>
  )
}
