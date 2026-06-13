import { useState, useEffect } from 'react'
import { wsClient } from './wsClient'
import { EventBus, EVT } from './EventBus'
import type { DishItem, MenuItem } from './types'

/**
 * 前台下单表单组件（升级版）
 *
 * 新增：
 *   - 菜单点选：点击菜名输入框弹出常用菜品列表，点一下直接填入
 *   - 加急开关：提交订单时可标记为加急
 *   - 简易菜单管理：在弹层内支持新增/删除菜品
 *
 * 职责：
 *   - 录入桌号、整体备注
 *   - 动态添加/删除多行菜品（菜名、数量、单条备注）
 *   - 提交时通过 wsClient.send({ type:'CREATE' }) 把订单发给后端
 */
export default function OrderEntry({ onSubmitted }: { onSubmitted?: () => void }) {
  const [tableNo, setTableNo] = useState('')
  const [remark, setRemark] = useState('')
  const [urgent, setUrgent] = useState(false)
  const [dishes, setDishes] = useState<Array<{ name: string; qty: number; note: string }>>([
    { name: '', qty: 1, note: '' },
  ])

  // 菜单数据 + 弹层状态
  const [menuList, setMenuList] = useState<MenuItem[]>([])
  const [menuPickerIdx, setMenuPickerIdx] = useState<number | null>(null)
  const [showMenuMgr, setShowMenuMgr] = useState(false)

  // 菜单管理弹层里的表单
  const [newMenuName, setNewMenuName] = useState('')
  const [newMenuCategory, setNewMenuCategory] = useState('')

  // 订阅菜单数据
  useEffect(() => {
    const unSub = EventBus.on<MenuItem[]>(EVT.MENU_UPDATED, (list) => list && setMenuList(list))
    return unSub
  }, [])

  // 提交
  const updateDish = (idx: number, patch: Partial<{ name: string; qty: number; note: string }>) => {
    setDishes((arr) => arr.map((d, i) => (i === idx ? { ...d, ...patch } : d)))
  }
  const addDish = () => setDishes((arr) => [...arr, { name: '', qty: 1, note: '' }])
  const removeDish = (idx: number) => setDishes((arr) => arr.filter((_, i) => i !== idx))

  const submit = () => {
    const validDishes = dishes
      .filter((d) => d.name.trim() && d.qty > 0)
      .map<DishItem>((d) => ({
        id: '', // 后端会补上 UUID
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
      urgent,
    })
    // 提交后清空
    setTableNo('')
    setRemark('')
    setUrgent(false)
    setDishes([{ name: '', qty: 1, note: '' }])
    onSubmitted?.()
  }

  // 从菜单选菜
  const pickMenuItem = (item: MenuItem) => {
    if (menuPickerIdx === null) return
    updateDish(menuPickerIdx, { name: item.name })
    setMenuPickerIdx(null)
  }

  // 添加新菜品到菜单
  const addMenu = () => {
    if (!newMenuName.trim()) return
    wsClient.send({
      type: 'MENU_ADD',
      item: {
        id: '',
        name: newMenuName.trim(),
        category: newMenuCategory.trim() || undefined,
      },
    })
    setNewMenuName('')
    setNewMenuCategory('')
  }

  const deleteMenu = (id: string) => {
    if (!confirm('确定删除这个菜品吗？')) return
    wsClient.send({ type: 'MENU_DELETE', id })
  }

  // 按分类分组
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
            <div style={{ position: 'relative', flex: 2 }}>
              <input
                placeholder="点击选菜 或 输入菜名"
                value={d.name}
                onFocus={() => setMenuPickerIdx(idx)}
                onChange={(e) => updateDish(idx, { name: e.target.value })}
              />
              {/* 菜单弹层 */}
              {menuPickerIdx === idx && (
                <div className="menu-picker">
                  <div className="picker-head">
                    <span>常用菜品</span>
                    <button
                      type="button"
                      className="btn-link"
                      onClick={() => { setMenuPickerIdx(null); setShowMenuMgr(true) }}
                    >
                      管理菜单
                    </button>
                    <button
                      type="button"
                      className="btn-link"
                      style={{ marginLeft: 8 }}
                      onClick={() => setMenuPickerIdx(null)}
                    >
                      关闭
                    </button>
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
                            <button
                              type="button"
                              key={item.id}
                              className="menu-chip"
                              onClick={() => pickMenuItem(item)}
                            >
                              {item.name}
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
              type="number"
              min={1}
              value={d.qty}
              style={{ flex: 1 }}
              onChange={(e) => updateDish(idx, { qty: Math.max(1, +e.target.value || 1) })}
            />
            <input
              placeholder="备注（少辣、去冰等，可选）"
              value={d.note}
              style={{ flex: 2 }}
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

      <div className="form-row" style={{ marginTop: 16 }}>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={urgent}
            onChange={(e) => setUrgent(e.target.checked)}
            style={{ transform: 'scale(1.2)' }}
          />
          <span style={{ color: urgent ? '#E65100' : '#5D4037', fontWeight: urgent ? 600 : 400 }}>
            🔥 加急订单（优先制作）
          </span>
        </label>
      </div>

      <div className="form-row" style={{ marginTop: 12 }}>
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

      <button type="submit" className={`btn-submit ${urgent ? 'btn-urgent' : ''}`}>
        {urgent ? '🔥 ' : '✅ '}
        提交订单到后厨
      </button>

      {/* 菜单管理弹层 */}
      {showMenuMgr && (
        <div className="modal-overlay" onClick={() => setShowMenuMgr(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-head">
              <h3>🍽️ 菜单管理</h3>
              <button className="btn-link" onClick={() => setShowMenuMgr(false)}>
                关闭
              </button>
            </div>
            <div className="modal-body">
              <div className="menu-mgr-add">
                <input
                  placeholder="菜品名称"
                  value={newMenuName}
                  onChange={(e) => setNewMenuName(e.target.value)}
                />
                <input
                  placeholder="分类（如热菜/凉菜/饮品）"
                  value={newMenuCategory}
                  onChange={(e) => setNewMenuCategory(e.target.value)}
                  style={{ width: 180 }}
                />
                <button type="button" className="btn btn-primary" onClick={addMenu}>
                  添加
                </button>
              </div>
              <div className="menu-mgr-list">
                {menuList.length === 0 && (
                  <div style={{ color: '#999', padding: '12px 0' }}>还没有菜品</div>
                )}
                {menuList.map((m) => (
                  <div key={m.id} className="menu-mgr-row">
                    <span style={{ flex: 1 }}>{m.name}</span>
                    <span style={{ color: '#888', fontSize: 13 }}>{m.category || '-'}</span>
                    <button
                      type="button"
                      className="btn btn-warn btn-sm"
                      onClick={() => deleteMenu(m.id)}
                    >
                      删除
                    </button>
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
