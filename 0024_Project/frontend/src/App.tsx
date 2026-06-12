import { useEffect, useState } from 'react'
import { EventBus, EVT } from './EventBus'
import { wsClient } from './wsClient'
import type { Order } from './types'
import OrderEntry from './OrderEntry'
import KitchenBoard from './KitchenBoard'

/**
 * 根组件
 *
 * 职责：
 *  1. 启动 WebSocket 连接（wsClient.start）
 *  2. 通过 EventBus 订阅最新订单列表，放到局部 state
 *  3. 顶部导航：切换 "前台下单 / 后厨看板" 两个视图
 *  4. 全局搜索框（两个视图都能共用）
 *  5. 显示连接状态 & Toast 提示
 *
 * 调用关系：
 *   App
 *    ├─ OrderEntry    (下单表单，调用 wsClient.send CREATE)
 *    └─ KitchenBoard  (三列看板，内部渲染 3 个 OrderColumn)
 *         └─ OrderColumn x 3
 *              └─ OrderDetail  (每张卡片，wsClient.send START/FINISH/REDO)
 */
export default function App() {
  const [view, setView] = useState<'entry' | 'board'>('board')
  const [orders, setOrders] = useState<Order[]>([])
  const [keyword, setKeyword] = useState('')
  const [wsState, setWsState] = useState<'connecting' | 'open' | 'closed'>('connecting')
  const [toast, setToast] = useState<string | null>(null)

  // --- 启动 WebSocket & 订阅事件 ---
  useEffect(() => {
    wsClient.start()
    const unSub1 = EventBus.on<Order[]>(EVT.ORDERS_UPDATED, (list) => {
      setOrders(list ?? [])
    })
    const unSub2 = EventBus.on<'connecting' | 'open' | 'closed'>(EVT.WS_STATUS, (s) => {
      setWsState(s)
      if (s === 'open') showToast('已连接服务端')
      if (s === 'closed') showToast('连接断开，正在重连...')
    })
    return () => {
      unSub1(); unSub2()
      wsClient.close()
    }
  }, [])

  // --- 广播搜索关键字给 KitchenBoard（也可以直接 props 传，这里演示 EventBus）---
  useEffect(() => {
    EventBus.emit(EVT.SEARCH_CHANGED, keyword.trim())
  }, [keyword])

  function showToast(msg: string) {
    setToast(msg)
    setTimeout(() => setToast(null), 2200)
  }

  const onSwitch = (v: 'entry' | 'board') => {
    setView(v)
    EventBus.emit(EVT.VIEW_CHANGED, v)
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header className="top-bar">
        <h1>🍳 实时厨房出餐屏</h1>

        <nav className="view-switch">
          <button
            className={view === 'entry' ? 'active' : ''}
            onClick={() => onSwitch('entry')}
          >
            📝 前台下单
          </button>
          <button
            className={view === 'board' ? 'active' : ''}
            onClick={() => onSwitch('board')}
          >
            👨‍🍳 后厨看板
          </button>
        </nav>

        <div className="ws-status">
          <span className={`ws-dot ${wsState}`}></span>
          {wsState === 'open' && '已连接'}
          {wsState === 'connecting' && '连接中...'}
          {wsState === 'closed' && '已断开'}
        </div>
      </header>

      {view === 'board' && (
        <div className="main-container">
          <div className="search-bar">
            <span className="search-label">🔍 搜索：</span>
            <input
              placeholder="输入桌号或订单号，如 A3 / 1005"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
            <span style={{ fontSize: 13, color: '#8B6F47' }}>
              共 {orders.length} 个订单
            </span>
          </div>
          <KitchenBoard allOrders={orders} />
        </div>
      )}

      {view === 'entry' && (
        <div className="main-container">
          <OrderEntry onSubmitted={() => showToast('✅ 订单已发送到后厨')} />
        </div>
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  )
}
