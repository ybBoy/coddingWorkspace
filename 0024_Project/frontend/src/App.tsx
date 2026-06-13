import { useEffect, useState } from 'react'
import { EventBus, EVT } from './EventBus'
import { wsClient } from './wsClient'
import type { Order, MenuItem, Role } from './types'
import OrderEntry from './OrderEntry'
import KitchenBoard from './KitchenBoard'
import TodayStats from './TodayStats'
import NotificationManager from './NotificationManager'
import HistoryArchive from './HistoryArchive'
import RoleSelector from './RoleSelector'

type View = 'entry' | 'board' | 'history'

const ROLE_KEY = 'kitchen_role'

function loadRole(): Role {
  return (localStorage.getItem(ROLE_KEY) as Role) || 'OWNER'
}

export default function App() {
  const [view, setView] = useState<View>('board')
  const [orders, setOrders] = useState<Order[]>([])
  const [menuList, setMenuList] = useState<MenuItem[]>([])
  const [keyword, setKeyword] = useState('')
  const [wsState, setWsState] = useState<'connecting' | 'open' | 'closed'>('connecting')
  const [toast, setToast] = useState<string | null>(null)
  const [bigScreen, setBigScreen] = useState<boolean>(() => localStorage.getItem('bigscreen') === '1')
  const [pendingCount, setPendingCount] = useState(0)
  const [role, setRole] = useState<Role>(loadRole)

  useEffect(() => {
    localStorage.setItem('bigscreen', bigScreen ? '1' : '0')
    if (bigScreen) document.body.classList.add('bigscreen')
    else document.body.classList.remove('bigscreen')
  }, [bigScreen])

  useEffect(() => {
    wsClient.start()

    const unSub1 = EventBus.on<Order[]>(EVT.ORDERS_UPDATED, (list) => {
      setOrders(list ?? [])
      setPendingCount(wsClient.pendingCount)
    })
    const unSub2 = EventBus.on<'connecting' | 'open' | 'closed'>(EVT.WS_STATUS, (s) => {
      setWsState(s)
      if (s === 'open') showToast('已连接服务端')
      if (s === 'closed') showToast('连接断开，正在重连...')
    })
    const unSub3 = EventBus.on<MenuItem[]>(EVT.MENU_UPDATED, (list) => setMenuList(list ?? []))
    const unSub4 = EventBus.on<View>(EVT.VIEW_CHANGED, (v) => { if (v) setView(v) })

    const t = setInterval(() => setPendingCount(wsClient.pendingCount), 2000)
    return () => { unSub1(); unSub2(); unSub3(); unSub4(); clearInterval(t); wsClient.close() }
  }, [])

  useEffect(() => {
    EventBus.emit(EVT.SEARCH_CHANGED, keyword.trim())
  }, [keyword])

  useEffect(() => {
    localStorage.setItem(ROLE_KEY, role)
    EventBus.emit(EVT.ROLE_CHANGED, role)
  }, [role])

  function showToast(msg: string) {
    setToast(msg)
    setTimeout(() => setToast(null), 2200)
  }

  const onSwitch = (v: View) => {
    setView(v)
    EventBus.emit(EVT.VIEW_CHANGED, v)
  }

  const toggleBigScreen = () => setBigScreen((v) => !v)
  const onRoleChange = (r: Role) => setRole(r)

  // 权限判断
  const canEntry = role === 'OWNER' || role === 'FRONT'
  const canHistory = role === 'OWNER'

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {wsState === 'closed' && (
        <div className="offline-banner">
          <span>网络已断开，正在自动重连...</span>
          {pendingCount > 0 && (
            <span style={{ marginLeft: 16 }}>有 {pendingCount} 条操作暂存，重连后自动发送</span>
          )}
        </div>
      )}

      <header className="top-bar">
        <h1>实时厨房出餐屏</h1>

        <nav className="view-switch">
          {canEntry && (
            <button className={view === 'entry' ? 'active' : ''} onClick={() => onSwitch('entry')}>
              前台下单
            </button>
          )}
          <button className={view === 'board' ? 'active' : ''} onClick={() => onSwitch('board')}>
            后厨看板
          </button>
          {canHistory && (
            <button className={view === 'history' ? 'active' : ''} onClick={() => onSwitch('history')}>
              历史归档
            </button>
          )}
        </nav>

        <div className="top-right">
          <RoleSelector role={role} onChange={onRoleChange} />
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
            <span className="search-label">搜索：</span>
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

      {view === 'entry' && canEntry && (
        <div className="main-container">
          <OrderEntry onSubmitted={() => showToast('订单已发送到后厨')} />
        </div>
      )}

      {view === 'history' && canHistory && (
        <div className="main-container">
          <HistoryArchive role={role} />
        </div>
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  )
}
