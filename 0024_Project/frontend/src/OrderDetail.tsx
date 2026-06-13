import { useEffect, useState } from 'react'
import type { Order } from './types'
import { wsClient } from './wsClient'
import { EventBus, EVT } from './EventBus'

/**
 * 单张订单卡片（升级版）
 *
 * 新增：
 *   - station 标签：每道菜显示制作工位
 *   - highlightStation：当前选中工位时高亮对应菜品，其他变淡
 *   - CANCELLED 灰色样式 + 撤销原因
 *   - 改单按钮：emit EDIT_ORDER 事件（跳到下单页填好数据）
 *   - 撤单按钮
 *   - 打印小票按钮
 */
interface Props {
  order: Order
  column: 'new' | 'cooking' | 'done'
  highlightStation?: string | null
}

/** 格式化时间 HH:mm */
function fmtTime(ts: number): string {
  const d = new Date(ts)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function isTimeout(o: Order): boolean {
  if (o.status === 'DONE' || o.status === 'CANCELLED') return false
  return Date.now() - o.createdAt > 15 * 60 * 1000
}

function elapsed(ts: number): number {
  return Math.floor((Date.now() - ts) / 60000)
}

function pad(n: number): string { return String(n).padStart(2, '0') }
function formatFull(ts: number): string {
  const d = new Date(ts)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 订单小票打印（新窗口 + window.print） */
function printReceipt(order: Order) {
  const items: string[] = []
  items.push(`<div style="font-family: monospace; width: 320px; padding: 10px; font-size: 14px;">`)
  items.push(`<div style="text-align:center; font-size:18px; font-weight:bold; margin-bottom: 6px;">厨房出餐小票</div>`)
  items.push(`<div style="border-bottom: 1px dashed #999; padding-bottom: 6px; margin-bottom: 6px;">`)
  items.push(`<div>订单号：${order.id}</div>`)
  items.push(`<div>桌号：${order.tableNo}桌</div>`)
  items.push(`<div>下单时间：${formatFull(order.createdAt)}</div>`)
  if (order.priority === 'HIGH') items.push(`<div style="color:#d32f2f;">加急订单</div>`)
  items.push(`</div>`)
  items.push(`<div style="border-bottom: 1px dashed #999; padding: 6px 0;">`)
  for (const d of order.dishes) {
    const extra = [
      d.note ? `(${d.note})` : '',
      d.redo ? '[重做]' : '',
      d.station ? `[${d.station}]` : '',
    ].join('')
    items.push(
      `<div style="display:flex; justify-content:space-between;">
        <span>${d.name}${extra}</span><span>x${d.quantity}</span>
      </div>`
    )
  }
  items.push(`</div>`)
  if (order.remark) items.push(`<div style="padding-top: 6px; border-top: 1px dashed #999;">备注：${order.remark}</div>`)
  if (order.cancelReason) items.push(`<div style="color:#999;">撤销原因：${order.cancelReason}</div>`)
  items.push(`<div style="margin-top:12px; text-align:center; color:#666; font-size: 12px;">— 谢谢惠顾 —</div>`)
  items.push(`</div>`)

  const w = window.open('', '_blank', 'width=360,height=600')
  if (!w) {
    alert('浏览器阻止了弹出窗口，请允许弹出后再试')
    return
  }
  const html = `<!DOCTYPE html><html><head><title>小票 ${order.id}</title>
    <style>
      body { margin: 0; padding: 10px; font-family: monospace; }
      @media print { @page { margin: 0; size: 80mm auto; } }
    </style></head><body>${items.join('')}</body></html>`
  w.document.write(html)
  w.document.close()
  setTimeout(() => { w.focus(); w.print(); setTimeout(() => w.close(), 500) }, 300)
}

export default function OrderDetail({ order, column, highlightStation }: Props) {
  const [, setTick] = useState(0)
  useEffect(() => {
    const t = setInterval(() => setTick((v) => v + 1), 30000)
    return () => clearInterval(t)
  }, [])

  const timeout = isTimeout(order)
  const mins = elapsed(order.createdAt)
  const hasRedo = order.dishes.some((d) => d.redo)
  const isUrgent = order.priority === 'HIGH'
  const cancelled = order.status === 'CANCELLED'

  const doneCount = order.dishes.filter((d) => d.done).length
  const totalCount = order.dishes.length

  const onStart = () => wsClient.send({ type: 'START', orderId: order.id })
  const onFinish = () => wsClient.send({ type: 'FINISH', orderId: order.id })
  const onToggleDish = (dishId: string, done: boolean) => {
    wsClient.send({ type: 'DISH_DONE', orderId: order.id, dishId, done })
  }
  const onRedo = (dishId: string, isRedo: boolean) => {
    wsClient.send({
      type: isRedo ? 'UNREDO' : 'REDO',
      orderId: order.id,
      dishId,
    })
  }
  const onToggleUrgent = () => {
    wsClient.send({ type: 'SET_URGENT', orderId: order.id, urgent: !isUrgent })
  }
  const onCancel = () => {
    const reason = prompt('请输入撤销原因（可选）')
    if (reason === null) return
    wsClient.send({ type: 'CANCEL', orderId: order.id, reason })
  }
  const onEdit = () => {
    EventBus.emit(EVT.VIEW_CHANGED, 'entry')
    setTimeout(() => EventBus.emit(EVT.EDIT_ORDER, order), 100)
  }
  const onPrint = () => printReceipt(order)

  const cardClass = [
    'order-card',
    timeout ? 'timeout' : '',
    isUrgent ? 'urgent' : '',
    cancelled ? 'cancelled' : '',
  ].filter(Boolean).join(' ')

  return (
    <div className={cardClass}>
      <div className="card-head">
        <div>
          <span className="table-no">{order.tableNo}</span>
          {cancelled && <span className="tag tag-cancelled">已撤销</span>}
          {!cancelled && isUrgent && <span className="tag tag-urgent">急单</span>}
          {!cancelled && timeout && <span className="tag tag-timeout">超时 {mins}分</span>}
          {!cancelled && hasRedo && column !== 'done' && <span className="tag tag-redo">含重做</span>}
          {column === 'done' && !cancelled && <span className="tag tag-done">已出餐</span>}
          <div className="order-id">单号：{order.id}</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 20, fontWeight: 700, color: '#BF360C' }}>
            {totalCount}<span style={{ fontSize: 13, fontWeight: 400, color: '#795548' }}> 道</span>
          </div>
          {!cancelled && column !== 'done' && (
            <div style={{ fontSize: 12, color: '#6D4C41', marginTop: 2 }}>
              已做 {doneCount}/{totalCount}
            </div>
          )}
        </div>
      </div>

      <div className="card-meta">
        <span>下单：{fmtTime(order.createdAt)}</span>
        <span>
          已过 <strong style={{ color: mins > 10 ? '#E53935' : '#5D4037' }}>{mins} 分钟</strong>
        </span>
      </div>

      {cancelled && order.cancelReason && (
        <div className="order-remark" style={{ borderLeftColor: '#9E9E9E' }}>
          撤销原因：{order.cancelReason}
        </div>
      )}
      {!cancelled && order.remark && <div className="order-remark">📌 {order.remark}</div>}

      <div className="dishes">
        {order.dishes.map((d) => {
          const match = !highlightStation || !d.station || d.station === highlightStation
          const dim = highlightStation && d.station && d.station !== highlightStation
          const cls = [
            'dish-row',
            d.redo ? 'redo-row' : '',
            d.done ? 'dish-done' : '',
            dim ? 'dish-dim' : '',
          ].filter(Boolean).join(' ')
          return (
            <div key={d.id} className={cls}>
              <span className="dish-name" style={{ flex: 1 }}>
                {column === 'cooking' && !cancelled && (
                  <input
                    type="checkbox"
                    checked={!!d.done}
                    disabled={cancelled}
                    onChange={(e) => onToggleDish(d.id, e.target.checked)}
                    style={{ marginRight: 8, transform: 'scale(1.2)' }}
                  />
                )}
                {d.redo && <span className="tag tag-redo" style={{ marginRight: 6 }}>重做</span>}
                {d.station && match && (
                  <span className="station-tag" title={`${d.station}工位`}>{d.station}</span>
                )}
                {d.name}
                {d.note && <span className="dish-note">（{d.note}）</span>}
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span className="dish-qty">×{d.quantity}</span>
                {column === 'cooking' && !cancelled && (
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
          )
        })}
      </div>

      <div className="card-actions">
        {column === 'new' && !cancelled && (
          <>
            <button className="btn btn-primary" onClick={onStart}>开始制作</button>
            <button className={`btn ${isUrgent ? 'btn-warn' : 'btn-default'}`} onClick={onToggleUrgent}>
              {isUrgent ? '取消加急' : '加急'}
            </button>
            <button className="btn btn-default" onClick={onEdit}>改单</button>
            <button className="btn btn-default" onClick={onPrint}>打印</button>
            <button className="btn btn-default btn-danger" onClick={onCancel}>撤单</button>
          </>
        )}
        {column === 'cooking' && !cancelled && (
          <>
            <button className="btn btn-success" onClick={onFinish}>完成出餐</button>
            <button className={`btn ${isUrgent ? 'btn-warn' : 'btn-default'}`} onClick={onToggleUrgent}>
              {isUrgent ? '取消加急' : '加急'}
            </button>
            <button className="btn btn-default" onClick={onEdit}>改单</button>
            <button className="btn btn-default" onClick={onPrint}>打印</button>
            <button className="btn btn-default btn-danger" onClick={onCancel}>撤单</button>
          </>
        )}
        {column === 'done' && (
          <>
            <span style={{ fontSize: 12, color: cancelled ? '#777' : '#2E7D32' }}>
              {cancelled
                ? `撤销：${fmtTime(order.cancelledAt || order.updatedAt)}`
                : `出餐：${fmtTime(order.finishedAt || order.updatedAt)}`}
            </span>
            <button className="btn btn-default" style={{ marginLeft: 'auto' }} onClick={onPrint}>
              打印
            </button>
          </>
        )}
      </div>
    </div>
  )
}
