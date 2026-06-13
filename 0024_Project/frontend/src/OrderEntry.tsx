import { useState, useEffect } from 'react'
import { wsClient } from './wsClient'
import { EventBus, EVT } from './EventBus'
import type { DishItem, MenuItem, Order } from './types'

/**
 * 前台下单表单组件（第三轮升级版）
 *
 * 新增：
 *   - 编辑模式（EDIT_ORDER 事件触发）：预填数据、发 UPDATE 消息、done=true 菜品只读名/数量、只能改备注
 *   - 菜单管理新增"制作工位"字段（热菜/饮品/主食/...）
 *   - 菜单管理按工位选择下拉
 */
interface DishFormRow {
  // 编辑模式下保留原菜品 id（后端按 id 识别改/删/加）
  origId?: string
  origDone?: boolean
  name: string
  qty: number
  note: string
  station?: string
}

const PRESET_STATIONS = ['热菜', '主食', '饮品', '凉菜', '小吃', '点心', '其他']

export default function OrderEntry({ onSubmitted }: { onSubmitted?: () => void }) {
  // 是否处于编辑模式
  const [editingId, setEditingId] = useState<string | null>(null)

  const [tableNo, setTableNo] = useState('')
  const [remark, setRemark] = useState('')
  const [urgent, setUrgent] = useState(false)
  const [dishes, setDishes] = useState<DishFormRow[]>([{ name: '', qty: 1, note: '' }])

  const [menuList, setMenuList] = useState<MenuItem[]>([])
  const [menuPickerIdx, setMenuPickerIdx] = useState<number | null>(null)
  const [showMenuMgr, setShowMenuMgr] = useState(false)

  const [newMenuName, setNewMenuName] = useState('')
  const [newMenuCategory, setNewMenuCategory] = useState('')
  const [newMenuStation, setNewMenuStation] = useState('')

  useEffect(() => {
    const unSub1 = EventBus.on<MenuItem[]>(EVT.MENU_UPDATED, (list) => list && setMenuList(list))
    // 收到改单事件 -> 进入编辑模式
    const unSub2 = EventBus.on<Order>(EVT.EDIT_ORDER, (o) => {
      if (!o) return
      setEditingId(o.id)
      setTableNo(o.tableNo)
      setRemark(o.remark ?? '')
      setUrgent(o.priority === 'HIGH')
      setDishes(
        o.dishes.map((d) => ({
          origId: d.id,
          origDone: !!d.done,
          name: d.name,
          qty: d.quantity,
          note: d.note ?? '',
          station: d.station,
        }))
      )
    })
    return () => { unSub1(); unSub2() }
  }, [])

  const updateDish = (idx: number, patch: Partial<DishFormRow>) => {
    setDishes((arr) => arr.map((d, i) => (i === idx ? { ...d, ...patch } : d)))
  }
  const addDish = () => setDishes((arr) => [...arr, { name: '', qty: 1, note: '' }])
  const removeDish = (idx: number) => {
    const dish = dishes[idx]
    if (dish?.origDone) {
      alert('已完成的菜品不能删除')
      return
    }
    setDishes((arr) => (arr.length > 1 ? arr.filter((_, i) => i !== idx) : arr))
  }

  const exitEditMode = () => {
    setEditingId(null)
    setTableNo('')
    setRemark('')
    setUrgent(false)
    setDishes([{ name: '', qty: 1, note: '' }])
  }

  const submit = () => {
    const validDishes = dishes
      .filter((d) => d.name.trim() && d.qty > 0)
      .map<DishItem>((d) => ({
        id: d.origId ?? '',
        name: d.name.trim(),
        quantity: d.qty,
        note: d.note.trim() || undefined,
        station: d.station,
        done: d.origDone, // 原样传回已完成标记（后端会校验）
      }))

    if (!tableNo.trim()) { alert('请输入桌号'); return }
    if (validDishes.length === 0) { alert('至少添加一道菜品'); return }

    if (editingId) {
      wsClient.send({
        type: 'UPDATE',
        orderId: editingId,
        tableNo: tableNo.trim(),
        dishes: validDishes,
        remark: remark.trim() || undefined,
      })
      exitEditMode()
      onSubmitted?.()
    } else {
      wsClient.send({
        type: 'CREATE',
        tableNo: tableNo.trim(),
        dishes: validDishes,
        remark: remark.trim() || undefined,
        urgent,
      })
      exitEditMode()
      onSubmitted?.()
    }
  }

  const pickMenuItem = (item: MenuItem) => {
    if (menuPickerIdx === null) return
    const dish = dishes[menuPickerIdx]
    if (dish?.origDone) {
      alert('已完成的菜品不能修改名称')
      setMenuPickerIdx(null)
      return
    }
    updateDish(menuPickerIdx, {
      name: item.name,
      station: item.station,
    })
    setMenuPickerIdx(null)
  }

  const addMenu = () => {
    if (!newMenuName.trim()) return
    wsClient.send({
      type: 'MENU_ADD',
      item: {
        id: '',
        name: newMenuName.trim(),
        category: newMenuCategory.trim() || undefined,
        station: newMenuStation.trim() || undefined,
      },
    })
    setNewMenuName('')
    setNewMenuCategory('')
    setNewMenuStation('')
  }

  const deleteMenu = (id: string) => {
    if (!confirm('确定删除这个菜品吗？')) return
    wsClient.send({ type: 'MENU_DELETE', id })
  }

  const groupedMenu = menuList
    .filter((m) => m.enabled !== false)
    .reduce<Record<string, MenuItem[]>>((acc, m) => {
      const cat = m.category || '其他'
      if (!acc[cat]) acc[cat] = []
      acc[cat].push(m)
      return acc
    }, {})

  return (
    <form className="entry-form" onSubmit={(e) => { e.preventDefault(); submit() }}>
      <h2 className="form-title">
        {editingId ? '✏️ 修改订单' : '📝 录入新订单'}
        {editingId && (
          <button
            type="button"
            className="btn btn-default btn-sm"
            style={{ float: 'right', fontSize: 13 }}
            onClick={exitEditMode}
          >
            取消修改
          </button>
        )}
      </h2>

      {editingId && (
        <div className="edit-banner">
          正在修改订单 <strong>{editingId}</strong>。已完成（✓）的菜品不支持改名/改数量，仅可修改备注。
        </div>
      )}

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
          <div className={`dish-item ${d.origDone ? 'dish-item-done' : ''}`} key={idx}>
            <div style={{ position: 'relative', flex: 2 }}>
              <input
                placeholder="点击选菜 或 输入菜名"
                value={d.name}
                readOnly={d.origDone}
                className={d.origDone ? 'read-only' : ''}
                onFocus={() => !d.origDone && setMenuPickerIdx(idx)}
                onChange={(e) => !d.origDone && updateDish(idx, { name: e.target.value })}
              />
              {d.station && (
                <span className="station-tag station-tag-inline">{d.station}</span>
              )}
              {menuPickerIdx === idx && (
                <div className="menu-picker">
                  <div className="picker-head">
                    <span>常用菜品</span>
                    <button
                      type="button" className="btn-link"
                      onClick={() => { setMenuPickerIdx(null); setShowMenuMgr(true) }}
                    >
                      管理菜单
                    </button>
                    <button
                      type="button" className="btn-link" style={{ marginLeft: 8 }}
                      onClick={() => setMenuPickerIdx(null)}
                    >关闭</button>
                  </div>
                  {Object.keys(groupedMenu).length === 0 ? (
                    <div style={{ padding: 16, color: '#999', textAlign: 'center' }}>
                      暂无菜品，点击"管理菜单"添加
                    </div>
                  ) : (
                    Object.entries(groupedMenu).map(([cat, items]) => (
                      <div key={cat}>
                        <div className="picker-cat">{cat}</div>
                        <div className="picker-items">
                          {items.map((item) => (
                            <button type="button" key={item.id} className="menu-chip"
                              onClick={() => pickMenuItem(item)}>
                              {item.name}{item.station && <span style={{color:'#777'}}> · {item.station}</span>}
                            </button>
                          ))}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              )}
            </div>
            <input
              type="number" min={1} value={d.qty}
              readOnly={d.origDone} className={d.origDone ? 'read-only' : ''}
              style={{ flex: 1 }}
              onChange={(e) => !d.origDone && updateDish(idx, { qty: Math.max(1, +e.target.value || 1) })}
            />
            <input
              placeholder="备注（少辣、去冰等，可选）"
              value={d.note} style={{ flex: 2 }}
              onChange={(e) => updateDish(idx, { note: e.target.value })}
            />
            <button
              type="button" className="remove"
              onClick={() => removeDish(idx)}
              disabled={dishes.length <= 1 || d.origDone}
              title={d.origDone ? '已完成菜品不能删除' : '删除这道菜'}
            >
              {d.origDone ? '✓' : '✕'}
            </button>
          </div>
        ))}
        <button type="button" className="btn-add-dish" onClick={addDish}>＋ 再加一道菜</button>
      </div>

      {!editingId && (
        <div className="form-row" style={{ marginTop: 16 }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
            <input type="checkbox" checked={urgent}
              onChange={(e) => setUrgent(e.target.checked)}
              style={{ transform: 'scale(1.2)' }} />
            <span style={{ color: urgent ? '#E65100' : '#5D4037', fontWeight: urgent ? 600 : 400 }}>
              加急订单（优先制作）
            </span>
          </label>
        </div>
      )}

      <div className="form-row" style={{ marginTop: 12 }}>
        <label style={{ minWidth: '100%' }}>
          订单整体备注
          <textarea
            className="remark-area"
            placeholder="如：整单不要香菜、其中一份打包等"
            value={remark} onChange={(e) => setRemark(e.target.value)}
          />
        </label>
      </div>

      <button type="submit" className={`btn-submit ${urgent ? 'btn-urgent' : ''}`}>
        {editingId ? '💾 保存修改' : urgent ? '🔥 提交加急订单' : '✅ 提交订单到后厨'}
      </button>

      {showMenuMgr && (
        <div className="modal-overlay" onClick={() => setShowMenuMgr(false)}>
          <div className="modal modal-menu" onClick={(e) => e.stopPropagation()}>
            <div className="modal-head">
              <h3>菜单管理</h3>
              <button className="btn-link" onClick={() => setShowMenuMgr(false)}>关闭</button>
            </div>
            <div className="modal-body">
              <div className="menu-mgr-add">
                <input placeholder="菜品名称" value={newMenuName}
                  onChange={(e) => setNewMenuName(e.target.value)} />
                <input placeholder="分类（热菜/凉菜/饮品）" value={newMenuCategory}
                  onChange={(e) => setNewMenuCategory(e.target.value)} style={{ width: 180 }} />
                <select value={newMenuStation} onChange={(e) => setNewMenuStation(e.target.value)}>
                  <option value="">选择制作工位（可选）</option>
                  {PRESET_STATIONS.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
                <button type="button" className="btn btn-primary" onClick={addMenu}>添加</button>
              </div>
              <div className="menu-mgr-list">
                {menuList.length === 0 && (
                  <div style={{ color: '#999', padding: '12px 0' }}>还没有菜品</div>
                )}
                {menuList.map((m) => (
                  <div key={m.id} className="menu-mgr-row">
                    <span style={{ flex: 1 }}>{m.name}</span>
                    <span style={{ color: '#888', fontSize: 13 }}>{m.category || '-'}</span>
                    <span style={{ color: '#6D4C41', fontSize: 13 }}>{m.station ? `[${m.station}]` : ''}</span>
                    <button type="button" className="btn btn-warn btn-sm" onClick={() => deleteMenu(m.id)}>删除</button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </form>
  )
}
