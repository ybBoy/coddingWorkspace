import { useEffect, useState } from 'react'
import { EventBus, EVT } from './EventBus'
import { wsClient } from './wsClient'
import type { Order, DishAnalysis, Role } from './types'
import TodayStats from './TodayStats'

/**
 * 历史归档页面（第三轮新增）
 *
 * 功能：
 *   1. 顶部日期列表：所有存在归档 JSON 的日期按钮（从新到旧）
 *   2. 按日期查询：点击后发 HISTORY_QUERY -> 返回当日所有订单（内存+归档合并）
 *   3. 今日统计（复用 TodayStats，基于查询出来的订单）
 *   4. 菜品耗时分析：avgMinutes / 重做率 top 榜
 *   5. 导出 CSV 下载（UTF-8 BOM，Excel 可直接打开）
 *   6. 权限：仅 OWNER 可见导出和分析，FRONT/KITCHEN 只读列表
 */
interface Props {
  role: Role
}

function fmt(ts: number): string {
  const d = new Date(ts)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
function mins(ms: number): string {
  return (ms / 60000).toFixed(1) + ' 分'
}

export default function HistoryArchive({ role }: Props) {
  const [dates, setDates] = useState<string[]>([])
  const [currentDate, setCurrentDate] = useState<string | null>(null)
  const [orders, setOrders] = useState<Order[]>([])
  const [analysis, setAnalysis] = useState<DishAnalysis[]>([])
  const [loading, setLoading] = useState(false)

  // 进入页面或收到结果
  useEffect(() => {
    // 首次请求日期列表
    wsClient.send({ type: 'HISTORY_DATES' })
    const unSub1 = EventBus.on<string[]>(EVT.HISTORY_DATES, (d) => {
      if (!d) return
      setDates(d)
      // 默认选第一个（今天或最近一天）
      if (!currentDate && d.length > 0) {
        setCurrentDate(d[0])
        wsClient.send({ type: 'HISTORY_QUERY', date: d[0] })
        wsClient.send({ type: 'DISH_ANALYSIS', date: d[0] })
      }
    })
    const unSub2 = EventBus.on<{ date: string; orders: Order[] }>(EVT.HISTORY_RESULT, (p) => {
      if (!p) return
      setOrders(p.orders || [])
      setLoading(false)
    })
    const unSub3 = EventBus.on<{ date: string; analysis: DishAnalysis[] }>(EVT.DISH_ANALYSIS_RESULT, (p) => {
      if (!p) return
      setAnalysis(p.analysis || [])
    })
    const unSub4 = EventBus.on<{ date: string; csv: string }>(EVT.EXPORT_CSV_RESULT, (p) => {
      if (!p?.csv) return
      const blob = new Blob([p.csv], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `orders_${p.date || 'today'}.csv`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      setTimeout(() => URL.revokeObjectURL(url), 5000)
    })
    return () => { unSub1(); unSub2(); unSub3(); unSub4() }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const pickDate = (d: string) => {
    setCurrentDate(d)
    setLoading(true)
    setOrders([])
    setAnalysis([])
    wsClient.send({ type: 'HISTORY_QUERY', date: d })
    wsClient.send({ type: 'DISH_ANALYSIS', date: d })
  }

  const exportCsv = () => {
    if (!currentDate) return
    wsClient.send({ type: 'EXPORT_CSV', date: currentDate })
  }

  const isOwner = role === 'OWNER'

  return (
    <div className="history-page">
      <div className="history-head">
        <h2>历史归档</h2>
        <div className="history-sub">
          已终结（已出餐 / 已撤销）的订单按天归档，重启服务也不会丢失。
        </div>
      </div>

      {dates.length === 0 ? (
        <div className="empty-state">
          还没有任何归档数据（正常运营一天后会自动生成）
        </div>
      ) : (
        <>
          <div className="date-tabs">
            {dates.map((d) => (
              <button
                key={d}
                className={`date-tab ${currentDate === d ? 'active' : ''}`}
                onClick={() => pickDate(d)}
              >
                {d}
              </button>
            ))}
          </div>

          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center', margin: '16px 0' }}>
            <span style={{ fontWeight: 600 }}>当前日期：<span style={{ color: '#E65100' }}>{currentDate}</span></span>
            <span style={{ color: '#666' }}>共 {orders.length} 个订单</span>
            {isOwner && currentDate && (
              <button className="btn btn-primary btn-sm" onClick={exportCsv} style={{ marginLeft: 'auto' }}>
                导出 CSV
              </button>
            )}
          </div>

          <TodayStats orders={orders} />

          {isOwner && analysis.length > 0 && (
            <div className="analysis-panel">
              <h3>菜品耗时与重做分析（仅店主可见）</h3>
              <div className="analysis-grid">
                {analysis.slice(0, 10).map((a, i) => (
                  <div key={a.name} className="analysis-card">
                    <div className="analysis-rank">{i + 1}</div>
                    <div className="analysis-body">
                      <div className="analysis-name">{a.name}</div>
                      <div className="analysis-meta">
                        制作 <strong>{a.count}</strong> 份 ·
                        平均 <strong style={{ color: +a.avgMinutes > 12 ? '#c62828' : '#2e7d32' }}>
                          {a.avgMinutes.toFixed(1)} 分钟
                        </strong>
                      </div>
                      {a.redoCount > 0 && (
                        <div className="analysis-redo">
                          重做 {a.redoCount} 份（重做率 {a.redoRate.toFixed(1)}%）
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {loading ? (
            <div className="empty-state">加载中...</div>
          ) : orders.length === 0 ? (
            <div className="empty-state">这一天还没有订单</div>
          ) : (
            <div className="history-list">
              <div className="history-table">
                <div className="history-row history-head-row">
                  <div style={{ flex: '0 0 120px' }}>订单号</div>
                  <div style={{ flex: '0 0 70px' }}>桌号</div>
                  <div style={{ flex: '0 0 80px' }}>状态</div>
                  <div style={{ flex: '0 0 100px' }}>下单时间</div>
                  <div style={{ flex: '0 0 80px' }}>耗时</div>
                  <div style={{ flex: 1 }}>菜品明细</div>
                  <div style={{ flex: '0 0 100px' }}>备注</div>
                </div>
                {orders.map((o) => {
                  const cookMs = o.status === 'DONE' ? (o.finishedAt || o.updatedAt) - o.createdAt : 0
                  const done = o.status === 'DONE'
                  const cancelled = o.status === 'CANCELLED'
                  return (
                    <div key={o.id} className={`history-row ${cancelled ? 'row-cancelled' : ''}`}>
                      <div style={{ flex: '0 0 120px', fontFamily: 'monospace', fontSize: 12 }}>{o.id}</div>
                      <div style={{ flex: '0 0 70px', fontWeight: 600 }}>{o.tableNo}</div>
                      <div style={{ flex: '0 0 80px' }}>
                        <span className={`tag ${done ? 'tag-done' : cancelled ? 'tag-cancelled' : ''}`}>
                          {done ? '已出餐' : cancelled ? '已撤销' : o.status}
                        </span>
                      </div>
                      <div style={{ flex: '0 0 100px', fontSize: 12, color: '#666' }}>{fmt(o.createdAt)}</div>
                      <div style={{ flex: '0 0 80px', fontSize: 12, color: done ? '#2e7d32' : '#777' }}>
                        {cancelled ? '—' : done ? mins(cookMs) : '进行中'}
                      </div>
                      <div style={{ flex: 1, fontSize: 13 }}>
                        {o.dishes.map((d, i) => (
                          <span key={d.id} className="dish-inline">
                            {d.redo && <span className="tag tag-redo" style={{ marginRight: 4 }}>重做</span>}
                            {d.name}×{d.quantity}
                            {d.note && <em style={{ color: '#888' }}>（{d.note}）</em>}
                            {i < o.dishes.length - 1 && '、'}
                          </span>
                        ))}
                      </div>
                      <div style={{ flex: '0 0 100px', fontSize: 12, color: cancelled ? '#777' : '#555' }}>
                        {cancelled && o.cancelReason
                          ? `❌ ${o.cancelReason}`
                          : o.remark || ''}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
