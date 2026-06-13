import { useState, useEffect } from 'react'
import StatsCards from '../components/StatsCards'
import RecentList from '../components/RecentList'
import type { Booth, CheckInRecord, SnapshotData } from '../core/socket'
import { socket } from '../core/socket'
import {
  eventBus,
  EVENT_STATS_REFRESH,
  EVENT_RECORDS_UPDATE,
  EVENT_FILTER_CHANGE,
} from '../core/EventBus'

function DashboardPage() {
  const [currentTime, setCurrentTime] = useState(new Date())
  const [booths, setBooths] = useState<Booth[]>([])
  const [records, setRecords] = useState<CheckInRecord[]>([])
  const [boothStats, setBoothStats] = useState<Record<string, number>>({})
  const [projectStats, setProjectStats] = useState<Record<string, number>>({})
  const [peakBooths, setPeakBooths] = useState<string[]>([])
  const [filterBoothId, setFilterBoothId] = useState<string | undefined>(
    undefined
  )

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date())
    }, 1000)
    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    const handleStatsRefresh = (data: SnapshotData) => {
      if (!data) return
      if (data.booths) setBooths(data.booths)
      if (data.boothStats) setBoothStats(data.boothStats)
      if (data.projectStats) setProjectStats(data.projectStats)
      if (data.peakBooths) setPeakBooths(data.peakBooths)
      if (data.records) setRecords(data.records)
    }

    const handleRecordsUpdate = (newRecords: CheckInRecord[]) => {
      if (Array.isArray(newRecords)) {
        setRecords(newRecords)
      }
    }

    const handleFilterChange = (boothId?: string) => {
      setFilterBoothId(boothId)
    }

    const unsub1 = eventBus.on(EVENT_STATS_REFRESH, handleStatsRefresh)
    const unsub2 = eventBus.on(EVENT_RECORDS_UPDATE, handleRecordsUpdate)
    const unsub3 = eventBus.on(EVENT_FILTER_CHANGE, handleFilterChange)

    socket.connect()
    socket.requestStats()

    return () => {
      unsub1()
      unsub2()
      unsub3()
    }
  }, [])

  const formatTime = (date: Date): string => {
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    })
  }

  const sortedBoothStats = Object.entries(boothStats)
    .map(([boothId, count]) => {
      const booth = booths.find((b) => b.id === boothId)
      return {
        id: boothId,
        name: booth?.name || boothId,
        count,
        isPeak: peakBooths.includes(boothId),
      }
    })
    .sort((a, b) => b.count - a.count)

  const maxBoothCount =
    sortedBoothStats.length > 0
      ? Math.max(...sortedBoothStats.map((b) => b.count), 1)
      : 1

  const sortedProjectStats = Object.entries(projectStats)
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)

  const maxProjectCount =
    sortedProjectStats.length > 0
      ? Math.max(...sortedProjectStats.map((p) => p.count), 1)
      : 1

  return (
    <div>
      <div className="dashboard-header">
        <div>
          <h1 className="page-title">实时人流看板</h1>
          <p className="page-subtitle">展会签到数据实时监控与分析</p>
        </div>
        <div className="current-time">{formatTime(currentTime)}</div>
      </div>

      <StatsCards
        boothStats={boothStats}
        projectStats={projectStats}
        booths={booths}
        peakBooths={peakBooths}
      />

      <div className="two-col-grid">
        <div className="card">
          <h2 className="card-title">展位人流排行</h2>
          <div className="progress-list">
            {sortedBoothStats.length === 0 ? (
              <div
                style={{
                  textAlign: 'center',
                  padding: '32px',
                  color: 'var(--text-secondary)',
                }}
              >
                暂无数据
              </div>
            ) : (
              sortedBoothStats.map((booth) => (
                <div key={booth.id} className="progress-item">
                  <div className="progress-header">
                    <span className="progress-name">
                      {booth.name}
                      {booth.isPeak && (
                        <span className="peak-tag">🔥人流高峰</span>
                      )}
                    </span>
                    <span className="progress-count">{booth.count}人</span>
                  </div>
                  <div className="progress-bar">
                    <div
                      className="progress-fill"
                      style={{
                        width: `${(booth.count / maxBoothCount) * 100}%`,
                      }}
                    />
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="card">
          <h2 className="card-title">项目兴趣分布</h2>
          <div className="progress-list">
            {sortedProjectStats.length === 0 ? (
              <div
                style={{
                  textAlign: 'center',
                  padding: '32px',
                  color: 'var(--text-secondary)',
                }}
              >
                暂无数据
              </div>
            ) : (
              sortedProjectStats.map((project) => (
                <div key={project.name} className="progress-item">
                  <div className="progress-header">
                    <span className="progress-name">{project.name}</span>
                    <span className="progress-count">{project.count}人</span>
                  </div>
                  <div className="progress-bar">
                    <div
                      className="progress-fill"
                      style={{
                        width: `${(project.count / maxProjectCount) * 100}%`,
                      }}
                    />
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <div className="records-section">
        <RecentList
          records={records}
          booths={booths}
          filterBoothId={filterBoothId}
        />
      </div>
    </div>
  )
}

export default DashboardPage
