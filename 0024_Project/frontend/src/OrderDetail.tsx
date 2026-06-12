import { useEffect, useState } from 'react'
import type { Order } from './types'
import { wsClient } from './wsClient'

/**
 * 单张订单卡片（后厨看板里的最小单元）
 *
 * 职责：
 *   - 展示：桌号、订单号、菜品清单（含数量+单条备注）、订单整体备注、下单时间、耗时
 *   - 状态样式：超时红色边框+红色Tag；重做菜品橙色Tag；已出餐绿色卡片
 *   - 操作按钮：
 *       NEW 列    -> "开始制作"  （START -> COOKING）
 *       COOKING 列 -> "完成出餐" （FINISH -> DONE）
 *                  + 每个菜品旁 "重做" / "取消重做"
 *       DONE 列   -> 无操作按钮
 *
 * 注意：
 *   为了让"下单后过了X分钟"持续刷新，组件内挂了一个 30s 的 setInterval
 *   让 elapsedMin 状态定时更新（不依赖后端推送）
 */
interface Props {
  order: Order
  column: 'new' | 'cooking' | 'done'
}

/** 格式化时间 HH:mm */
function fmtTime(ts: number): string {
  const d = new Date(ts)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

/** 判断订单是否超时（>15分钟且未出餐） */
function isTimeout(o: Order): boolean {
  if (o.status === 'DONE') return false
  return Date.now() - o.createdAt > 15 * 60 * 1000
}

/** 下单经过的分钟数 */
function elapsed(ts: number): number {
  return Math.floor((Date.now() - ts) / 60000)
}

export default function OrderDetail({ order, column }: Props) {
  // 用一个 tick 状态触发重渲染，显示最新的"经过X分钟"
  const [, setTick] = useState(0)
  useEffect(() => {
    const t = setInterval(() => setTick((v) => v + 1), 30000)
    return () => clearInterval(t)
  }, [])

  const timeout = isTimeout(order)
  const mins = elapsed(order.createdAt)
  const hasRedo = order.dishes.some((d) => d.redo)

  const onStart = () => wsClient.send({ type: 'START', orderId: order.id })
  const onFinish = () => wsClient.send({ type: 'FINISH', orderId: order.id })
  const onRedo = (dishId: string, isRedo: boolean) => {
    wsClient.send({
      type: isRedo ? 'UNREDO' : 'REDO',
      orderId: order.id,
      dishId,
    })
  }

  return (
    <div className={`order-card ${timeout ? 'timeout' : ''}`}>
      <div className="card-head">
        <div>
          <span className="table-no">{order.tableNo}</span>
          {timeout && <span className="tag tag-timeout">⏰ 超时 {mins}分钟</span>}
          {hasRedo && column !== 'done' && <span className="tag tag-redo">🔁 含重做</span>}
          {column === 'done' && <span className="tag tag-done">✓ 已出餐</span>}
          <div className="order-id">单号：{order.id}</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 20, fontWeight: 700, color: '#BF360C' }}>
            {order.dishes.reduce((s, d) => s + d.quantity, 0)}
            <span style={{ fontSize: 13, fontWeight: 400, color: '#795548' }}> 道</span>
          </div>
        </div>
      </div>

      <div className="card-meta">
        <span>下单：{fmtTime(order.createdAt)}</span>
        <span>
          已过{' '}
          <strong style={{ color: mins > 10 ? '#E53935' : '#5D4037' }}>{mins} 分钟</strong>
        </span>
      </div>

      {order.remark && <div className="order-remark">📌 {order.remark}</div>}

      <div className="dishes">
        {order.dishes.map((d) => (
          <div key={d.id} className={`dish-row ${d.redo ? 'redo-row' : ''}`}>
            <span className="dish-name">
              {d.redo && <span className="tag tag-redo" style={{ marginRight: 6 }}>重做</span>}
              {d.name}
              {d.note && <span className="dish-note">（{d.note}）</span>}
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span className="dish-qty">×{d.quantity}</span>
              {column === 'cooking' && (
                <button
                  className={`btn ${d.redo ? 'btn-default' : 'btn-warn'}`}
                  style={{ padding: '3px 8px', fontSize: 12 }}
                  onClick={() => onRedo(d.id, !!d.redo)}
                >
                  {d.redo ? '取消重做' : '重做'}
                </button>
              )}
            </span>
          </div>
        ))}
      </div>

      <div className="card-actions">
        {column === 'new' && (
          <button className="btn btn-primary" onClick={onStart}>
            👨‍🍳 开始制作
          </button>
        )}
        {column === 'cooking' && (
          <button className="btn btn-success" onClick={onFinish}>
            ✅ 完成出餐
          </button>
        )}
        {column === 'done' && (
          <span style={{ fontSize: 12, color: '#2E7D32' }}>
            出餐时间：{fmtTime(order.updatedAt)}
          </span>
        )}
      </div>
    </div>
  )
}
