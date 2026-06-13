import type { Booth } from '../core/socket'

interface StatsCardsProps {
  boothStats: Record<string, number>
  projectStats: Record<string, number>
  booths: Booth[]
  peakBooths: string[]
  todayTotal?: number
}

function StatsCards({
  boothStats,
  projectStats,
  booths,
  peakBooths,
  todayTotal = 0,
}: StatsCardsProps) {
  const totalCheckins = Object.values(boothStats).reduce(
    (sum, count) => sum + count,
    0
  )

  const boothCount = booths.length

  const sortedProjects = Object.entries(projectStats).sort(
    (a, b) => b[1] - a[1]
  )
  const topProject = sortedProjects.length > 0 ? sortedProjects[0][0] : '暂无'

  const peakCount = peakBooths.length

  const cards = [
    {
      icon: '📅',
      value: todayTotal,
      label: '今日签到数',
    },
    {
      icon: '👥',
      value: totalCheckins,
      label: '累计签到人数',
    },
    {
      icon: '🏢',
      value: boothCount,
      label: '展位数量',
    },
    {
      icon: '🏆',
      value: topProject,
      label: '最热门项目',
    },
    {
      icon: '🔥',
      value: peakCount,
      label: '当前高峰展位',
    },
  ]

  return (
    <div className="stats-grid">
      {cards.map((card, index) => (
        <div key={index} className="stat-card">
          <div className="stat-icon">{card.icon}</div>
          <div className="stat-content">
            <div className="stat-value">{card.value}</div>
            <div className="stat-label">{card.label}</div>
          </div>
        </div>
      ))}
    </div>
  )
}

export default StatsCards
