import { useEffect, useState } from 'react'
import { EventBus, EVT } from './EventBus'
import { wsClient } from './wsClient'
import type { Order, MenuItem } from './types'
import OrderEntry from './OrderEntry'
import KitchenBoard from './KitchenBoard'
import TodayStats from './TodayStats'
import NotificationManager from './NotificationManager'

/**
 * 根组件（升级版）
 *
 * 新增：
 *   - 今日统计面板（看板顶部）
 *   - 大屏模式开关（字体卡片都放大，适合挂电视）
 *   - 离线提示 banner（WS 断开时显眼提示）
 *   - 新订单提醒按钮（声音 + 桌面通知）
 *   - 离线队列大小提示（有 N 条待发送）
 *
 * 职责：
 *  1. 启动 WebSocket 连接（wsClient.start）
 *  2. 通过 EventBus 订阅最新订单列表 + 菜单数据，放到局部 state
 *  3. 顶部导航：切换 "前台下单 / 后厨看板" 两个视图
 *  4. 全局搜索框（两个视图都能共用）
 *  5. 显示连接状态 & Toast 提示
 */
export default function App() {
  const [view, setView] = useState<'entry' | 'board'>('board')
  const [orders, setOrders] = useState<Order[]>([])
  const [menuList, setMenuList] = useState<MenuItem[]>([])
  const [keyword, setKeyword] = useState('')
  const [wsState, setWsState] = useState<'connecting' | 'open' | 'closed'>('connecting')
  const [toast, setToast] = useState<string | null>(null)
  const [bigScreen, setBigScreen] = useState<boolean>(() => {
    return localStorage.getItem('bigscreen') === '1'
  })
  const [pendingCount, setPendingCount] = useState(0)

  // --- 大屏模式持久化 + body class 切换 ---
  useEffect(() => {
    localStorage.setItem('bigscreen', bigScreen ? '1' : '0')
    if (bigScreen) document.body.classList.add('bigscreen')
    else document.body.classList.remove('bigscreen')
  }, [bigScreen])

  // --- 启动 WebSocket & 订阅事件 ---
  useEffect(() => {
    wsClient.start()

    const unSub1 = EventBus.on<Order[]>(EVT.ORDERS_UPDATED, (list) => {
      setOrders(list ?? [])
      // 每次收到订单数据时刷新一下 pending 数量
      setPendingCount(wsClient.pendingCount)
    })

    const unSub2 = EventBus.on<'connecting' | 'open' | 'closed'>(EVT.WS_STATUS, (s) => {
      setWsState(s)
      if (s === 'open') showToast('已连接服务端')
      if (s === 'closed') showToast('连接断开，正在重连...')
    })

    const unSub3 = EventBus.on<MenuItem[]>(EVT.MENU_UPDATED, (list) => {
      setMenuList(list ?? [])
    })

    // 每 2 秒轮询一下 pending 数量（有新消息入队时及时刷新 UI）
    const t = setInterval(() => {
      setPendingCount(wsClient.pendingCount)
    }, 2000)

    return () => {
      unSub1()
      unSub2()
      unSub3()
      clearInterval(t)
      wsClient.close()
    }
  }, [])

  // --- 广播搜索关键字给 KitchenBoard ---
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

  const toggleBigScreen = () => setBigScreen((v) => !v)

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* 离线横幅：连接断开时顶部醒目提示 */}
      {wsState === 'closed' && (
        <div className="offline-banner">
          <span>🔴 网络已断开，正在自动重连...</span>
          {pendingCount > 0 && (
            <span style={{ marginLeft: 16 }}>📦 有 {pendingCount} 条操作暂存，重连后自动发送</span>
          )}
        </div>
      )}

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

        <div className="top-right">
          <NotificationManager mode="icon" />
          <button
            className={`icon-btn ${bigScreen ? 'active' : ''}`}
            onClick={toggleBigScreen}
            title={bigScreen ? '退出大屏模式' : '进入大屏模式（适合电视）'}
          >
            🖥️
          </button>
          <div className="ws-status">
            <span className={`ws-dot ${wsState}`}></span>
            {wsState === 'open' && '已连接'}
            {wsState === 'connecting' && '连接中...'}
            {wsState === 'closed' && '已断开'}
          </div>
        </div>
      </header>

      {view === 'board' && (
        <div className="main-container">
          <TodayStats orders={orders} />
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
