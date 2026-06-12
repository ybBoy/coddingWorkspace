import { useState } from 'react'
import { wsClient } from './wsClient'
import type { DishItem } from './types'

/**
 * 前台下单表单组件
 *
 * 职责：
 *   - 录入桌号、整体备注
 *   - 动态添加/删除多行菜品（菜名、数量、单条备注）
 *   - 提交时通过 wsClient.send({ type:'CREATE' }) 把订单发给后端
 *   - 后端处理后通过 WebSocket 广播 ORDERS_UPDATED，所有页面同步刷新
 *
 * 调用链：
 *   OrderEntry.submit()
 *     -> wsClient.send(CREATE)
 *       -> KitchenSocket.onMessage(CREATE)  [后端]
 *         -> OrderService.createOrder()
 *           -> KitchenSocket.broadcastOrders()
 *             -> 所有前端 ws.onmessage
 *               -> EventBus.emit(ORDERS_UPDATED)
 *                 -> App / KitchenBoard 刷新
 */
export default function OrderEntry({ onSubmitted }: { onSubmitted?: () => void }) {
  const [tableNo, setTableNo] = useState('')
  const [remark, setRemark] = useState('')
  const [dishes, setDishes] = useState<Array<{ name: string; qty: number; note: string }>>([
    { name: '', qty: 1, note: '' },
  ])

  const updateDish = (idx: number, patch: Partial<{ name: string; qty: number; note: string }>) => {
    setDishes((arr) => arr.map((d, i) => (i === idx ? { ...d, ...patch } : d)))
  }
  const addDish = () => setDishes((arr) => [...arr, { name: '', qty: 1, note: '' }])
  const removeDish = (idx: number) => setDishes((arr) => arr.filter((_, i) => i !== idx))

  const submit = () => {
    const validDishes = dishes
      .filter((d) => d.name.trim() && d.qty > 0)
      .map<DishItem>((d) => ({
        id: '',   // 后端会补上 UUID
        name: d.name.trim(),
        quantity: d.qty,
        note: d.note.trim() || undefined,
      }))
    if (!tableNo.trim()) {
      alert('请输入桌号')
      return
    }
    if (validDishes.length === 0) {
      alert('至少添加一道菜品')
      return
    }
    wsClient.send({
      type: 'CREATE',
      tableNo: tableNo.trim(),
      dishes: validDishes,
      remark: remark.trim() || undefined,
    })
    // 提交后清空表单
    setTableNo('')
    setRemark('')
    setDishes([{ name: '', qty: 1, note: '' }])
    onSubmitted?.()
  }

  return (
    <form className="entry-form" onSubmit={(e) => { e.preventDefault(); submit() }}>
      <h2 className="form-title">📝 录入新订单</h2>

      <div className="form-row">
        <label>
          桌号
          <input
            placeholder="如 A3、12、外带01"
            value={tableNo}
            onChange={(e) => setTableNo(e.target.value)}
          />
        </label>
      </div>

      <div className="dish-list">
        <div style={{ marginBottom: 10, fontWeight: 600, color: '#5D4A2F' }}>🥘 菜品明细</div>
        {dishes.map((d, idx) => (
          <div className="dish-item" key={idx}>
            <input
              placeholder="菜名，如红烧牛肉面"
              value={d.name}
              onChange={(e) => updateDish(idx, { name: e.target.value })}
            />
            <input
              type="number"
              min={1}
              value={d.qty}
              onChange={(e) => updateDish(idx, { qty: Math.max(1, +e.target.value || 1) })}
            />
            <input
              placeholder="备注（少辣、去冰等，可选）"
              value={d.note}
              onChange={(e) => updateDish(idx, { note: e.target.value })}
            />
            <button
              type="button"
              className="remove"
              onClick={() => dishes.length > 1 && removeDish(idx)}
              disabled={dishes.length <= 1}
            >
              ✕
            </button>
          </div>
        ))}
        <button type="button" className="btn-add-dish" onClick={addDish}>
          ＋ 再加一道菜
        </button>
      </div>

      <div className="form-row" style={{ marginTop: 20 }}>
        <label style={{ minWidth: '100%' }}>
          订单整体备注
          <textarea
            className="remark-area"
            placeholder="如：整单不要香菜、其中一份打包等"
            value={remark}
            onChange={(e) => setRemark(e.target.value)}
          />
        </label>
      </div>

      <button type="submit" className="btn-submit">
        ✅ 提交订单到后厨
      </button>
    </form>
  )
}
