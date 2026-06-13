import { useEffect, useMemo, useState } from 'react'
import type { Order, OrderStatus } from './types'
import OrderColumn from './OrderColumn'
import { EventBus, EVT } from './EventBus'

/**
 * 后厨看板三列布局（升级版：支持多工位 Tab 切换）
 *
 * 职责：
 *   - 从父组件 App 拿到全量订单
 *   - 从 EventBus 订阅搜索关键字 + 工位切换
 *   - 顶部工位 Tab：全部 / 热菜 / 饮品 / 主食 / ...（从菜单 + 订单中收集所有工位）
 *   - 非"全部"工位时：
 *       · 只显示包含该工位菜品的订单
 *       · 高亮该工位的菜品，其他工位的变淡
 *   - 按状态把订单拆分成 3 列：新订单 / 制作中 / 已出餐（含已撤销单灰色）
 */
type ColDef = {
  key: OrderStatus
  title: string
  cls: 'new' | 'cooking' | 'done'
  icon: string
}

const COLS: ColDef[] = [
  { key: 'NEW',     title: '🆕 新订单',   cls: 'new',     icon: '🆕' },
  { key: 'COOKING', title: '🔥 制作中',   cls: 'cooking', icon: '🔥' },
  { key: 'DONE',    title: '✅ 已出餐',   cls: 'done',    icon: '✅' },
]

export default function KitchenBoard({ allOrders }: { allOrders: Order[] }) {
  const [keyword, setKeyword] = useState('')
  const [station, setStation] = useState<string | null>(null)

  useEffect(() => {
    const unSub1 = EventBus.on<string>(EVT.SEARCH_CHANGED, (k) => setKeyword(k ?? ''))
    const unSub2 = EventBus.on<string | null>(EVT.STATION_CHANGED, (s) => setStation(s ?? null))
    return () => { unSub1(); unSub2() }
  }, [])

  // 收集所有存在的工位（从订单 dishes 里 + 菜单配置里）
  const allStations = useMemo(() => {
    const set = new Set<string>()
    for (const o of allOrders) {
      for (const d of o.dishes) {
        if (d.station && d.station.trim()) set.add(d.station.trim())
      }
    }
    return Array.from(set).sort()
  }, [allOrders])

  // 按桌号/订单号过滤
  const kw = keyword.toLowerCase()
  const kwFiltered = kw
    ? allOrders.filter((o) =>
        o.id.toLowerCase().includes(kw) ||
        (o.tableNo ?? '').toLowerCase().includes(kw),
      )
    : allOrders

  // 按工位过滤：只保留包含该工位菜品的订单
  const stationFiltered = station
    ? kwFiltered.filter((o) => o.dishes.some((d) => d.station === station))
    : kwFiltered

  const byStatus = COLS.map((col) => {
    const list = stationFiltered
      .filter((o) => o.status === col.key)
      .sort((a, b) => {
        // 加急订单排前面
        if (a.priority !== b.priority) return a.priority === 'HIGH' ? -1 : 1
        return a.createdAt - b.createdAt
      })
    // CANCELLED 订单也丢到 DONE 列末尾，灰色显示
    let finalList = list
    if (col.key === 'DONE') {
      const cancelled = stationFiltered
        .filter((o) => o.status === 'CANCELLED')
        .sort((a, b) => b.createdAt - a.createdAt)
      finalList = [...list, ...cancelled]
    }
    return {
      col,
      list: finalList,
      // 真实总数（不带工位过滤）给列头部 badge 用
      totalCount: allOrders.filter((o) =>
        o.status === col.key ||
        (col.key === 'DONE' && o.status === 'CANCELLED')
      ).length,
    }
  })

  return (
    <>
      {/* 工位切换 Tab（只有检测到多工位时才显示） */}
      {allStations.length > 0 && (
        <div className="station-tabs">
          <button
            className={station === null ? 'active' : ''}
            onClick={() => EventBus.emit(EVT.STATION_CHANGED, null)}
          >
            📋 全部工位
          </button>
          {allStations.map((s) => (
            <button
              key={s}
              className={station === s ? 'active' : ''}
              onClick={() => EventBus.emit(EVT.STATION_CHANGED, s)}
            >
              🍳 {s}
            </button>
          ))}
        </div>
      )}

      {/* 把当前选中的工位传给 OrderColumn，内部用来高亮对应菜品 */}
      <div className="kitchen-board">
        {byStatus.map(({ col, list, totalCount }) => (
          <OrderColumn
            key={col.key}
            colClass={col.cls}
            title={col.title}
            orders={list}
            totalCount={totalCount}
            highlightStation={station}
          />
        ))}
      </div>
    </>
  )
}
