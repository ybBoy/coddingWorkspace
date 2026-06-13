import { useMemo } from 'react'
import type { Order } from './types'

/**
 * 今日统计面板
 *
 * 输入：所有订单列表
 * 输出：
 *   - 今日总订单数
 *   - 已出餐数量
 *   - 平均制作用时（分钟）
 *   - 超时订单数（>15 分钟且未出餐的 + 曾经超时过的已出餐订单）
 *   - 加急订单数
 *
 * 设计：
 *   - 放在看板顶部 / 侧栏，方便店主一眼看到今日经营概况
 *   - 用卡片式数字展示，暖色调
 */
interface Props {
  orders: Order[]
}

/** 判断订单是否超时（从下单到开始制作/出餐 > 15 分钟就算超时） */
function isOrderTimeout(o: Order): boolean {
  if (o.status === 'DONE') {
    const dur = (o.finishedAt || o.updatedAt) - o.createdAt
    return dur > 15 * 60 * 1000
  }
  return Date.now() - o.createdAt > 15 * 60 * 1000
}

export default function TodayStats({ orders }: Props) {
  const stats = useMemo(() => {
    // 过滤今日订单
    const todayStart = new Date()
    todayStart.setHours(0, 0, 0, 0)
    const todayTs = todayStart.getTime()
    const todayOrders = orders.filter((o) => o.createdAt >= todayTs)

    const total = todayOrders.length
    const doneCount = todayOrders.filter((o) => o.status === 'DONE').length
    const cookingCount = todayOrders.filter((o) => o.status === 'COOKING').length
    const newCount = todayOrders.filter((o) => o.status === 'NEW').length
    const urgentCount = todayOrders.filter((o) => o.priority === 'HIGH').length
    const timeoutCount = todayOrders.filter(isOrderTimeout).length

    // 平均制作时长（只算已完成的）
    const doneOrders = todayOrders.filter((o) => o.status === 'DONE')
    let avgMinutes = 0
    if (doneOrders.length > 0) {
      const totalMs = doneOrders.reduce(
        (sum, o) => sum + ((o.finishedAt || o.updatedAt) - o.createdAt),
        0,
      )
      avgMinutes = Math.round(totalMs / doneOrders.length / 60000)
    }

    return { total, doneCount, cookingCount, newCount, urgentCount, timeoutCount, avgMinutes }
  }, [orders])

  return (
    <div className="today-stats">
      <div className="stat-card stat-total">
        <div className="stat-label">今日总单</div>
        <div className="stat-value">{stats.total}</div>
      </div>
      <div className="stat-card stat-done">
        <div className="stat-label">已出餐</div>
        <div className="stat-value">{stats.doneCount}</div>
      </div>
      <div className="stat-card stat-cooking">
        <div className="stat-label">制作中</div>
        <div className="stat-value">{stats.cookingCount}</div>
      </div>
      <div className="stat-card stat-new">
        <div className="stat-label">待接单</div>
        <div className="stat-value">{stats.newCount}</div>
      </div>
      <div className="stat-card stat-avg">
        <div className="stat-label">平均用时</div>
        <div className="stat-value">{stats.avgMinutes} 分</div>
      </div>
      <div className="stat-card stat-timeout">
        <div className="stat-label">超时单</div>
        <div className="stat-value">{stats.timeoutCount}</div>
      </div>
      <div className="stat-card stat-urgent">
        <div className="stat-label">加急单</div>
        <div className="stat-value">{stats.urgentCount}</div>
      </div>
    </div>
  )
}
