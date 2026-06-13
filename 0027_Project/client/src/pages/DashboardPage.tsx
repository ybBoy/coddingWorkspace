import { useState, useEffect, useRef, useCallback } from 'react'
import StatsCards from '../components/StatsCards'
import RecentList from '../components/RecentList'
import type { Booth, CheckInRecord, SnapshotData, RangeStatsData } from '../core/socket'
import { socket } from '../core/socket'
import {
  eventBus,
  EVENT_STATS_REFRESH,
  EVENT_RECORDS_UPDATE,
  EVENT_FILTER_CHANGE,
  EVENT_RANGE_STATS,
  EVENT_PEAK_ALERT,
} from '../core/EventBus'

type TimeRange = '10min' | 'today' | 'all'

const RANGE_OPTIONS: { value: TimeRange; label: string }[] = [
  { value: '10min', label: '最近10分钟' },
  { value: 'today', label: '今天' },
  { value: 'all', label: '全部' },
]

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
  const [timeRange, setTimeRange] = useState<TimeRange>('all')
  const [todayTotal, setTodayTotal] = useState<number>(0)
  const [soundEnabled, setSoundEnabled] = useState(false)
  const prevPeakRef = useRef<Set<string>>(new Set())
  const audioCtxRef = useRef<AudioContext | null>(null)

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date())
    }, 1000)
    return () => clearInterval(timer)
  }, [])

  const playPeakAlert = useCallback(() => {
    if (!soundEnabled) return
    try {
      if (!audioCtxRef.current) {
        audioCtxRef.current = new (window.AudioContext ||
          (window as any).webkitAudioContext)()
      }
      const ctx = audioCtxRef.current
      const oscillator = ctx.createOscillator()
      const gainNode = ctx.createGain()
      oscillator.connect(gainNode)
      gainNode.connect(ctx.destination)
      oscillator.frequency.value = 880
      oscillator.type = 'sine'
      gainNode.gain.setValueAtTime(0.3, ctx.currentTime)
      gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.3)
      oscillator.start(ctx.currentTime)
      oscillator.stop(ctx.currentTime + 0.3)

      setTimeout(() => {
        const osc2 = ctx.createOscillator()
        const gain2 = ctx.createGain()
        osc2.connect(gain2)
        gain2.connect(ctx.destination)
        osc2.frequency.value = 1100
        osc2.type = 'sine'
        gain2.gain.setValueAtTime(0.3, ctx.currentTime)
        gain2.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.3)
        osc2.start(ctx.currentTime)
        osc2.stop(ctx.currentTime + 0.3)
      }, 350)
    } catch (e) {
      console.warn('Play alert sound failed:', e)
    }
  }, [soundEnabled])

  const detectNewPeaks = useCallback(
    (newPeakBooths: string[]) => {
      const newPeakSet = new Set(newPeakBooths)
      const newlyPeaked: string[] = []
      newPeakSet.forEach((id) => {
        if (!prevPeakRef.current.has(id)) {
          newlyPeaked.push(id)
        }
      })
      if (newlyPeaked.length > 0) {
        eventBus.emit(EVENT_PEAK_ALERT, newlyPeaked)
        playPeakAlert()
      }
      prevPeakRef.current = newPeakSet
    },
    [playPeakAlert]
  )

  useEffect(() => {
    const handleStatsRefresh = (data: SnapshotData) => {
      if (!data) return
      if (data.booths) setBooths(data.booths)
      if (data.peakBooths) {
        setPeakBooths(data.peakBooths)
        detectNewPeaks(data.peakBooths)
      }
      if (data.todayTotal !== undefined) setTodayTotal(data.todayTotal)
      if (timeRange === 'all') {
        if (data.boothStats) setBoothStats(data.boothStats)
        if (data.projectStats) setProjectStats(data.projectStats)
        if (data.records) setRecords(data.records)
      } else if (timeRange === 'today') {
        if (data.todayBoothStats) setBoothStats(data.todayBoothStats)
        if (data.todayProjectStats) setProjectStats(data.todayProjectStats)
        if (data.records) {
          const todayStart = new Date()
          todayStart.setHours(0, 0, 0, 0)
          setRecords(data.records.filter((r) => r.timestamp >= todayStart.getTime()))
        }
      }
    }

    const handleRecordsUpdate = (newRecords: CheckInRecord[]) => {
      if (Array.isArray(newRecords) && timeRange === 'all') {
        setRecords(newRecords)
      }
    }

    const handleFilterChange = (boothId?: string) => {
      setFilterBoothId(boothId)
    }

    const handleRangeStats = (data: RangeStatsData) => {
      if (data.range === timeRange) {
        if (data.records) setRecords(data.records)
        if (data.boothStats) setBoothStats(data.boothStats)
        if (data.projectStats) setProjectStats(data.projectStats)
      }
    }

    const unsub1 = eventBus.on(EVENT_STATS_REFRESH, handleStatsRefresh)
    const unsub2 = eventBus.on(EVENT_RECORDS_UPDATE, handleRecordsUpdate)
    const unsub3 = eventBus.on(EVENT_FILTER_CHANGE, handleFilterChange)
    const unsub4 = eventBus.on(EVENT_RANGE_STATS, handleRangeStats)

    socket.connect()
    socket.requestStats()

    return () => {
      unsub1()
      unsub2()
      unsub3()
      unsub4()
    }
  }, [timeRange, detectNewPeaks])

  useEffect(() => {
    if (timeRange === '10min' || timeRange === 'today') {
      socket.requestRecordsByRange(timeRange)
    } else {
      socket.requestStats()
    }
  }, [timeRange])

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

  const exportCSV = () => {
    const rows = [
      ['签到时间', '展位', '姓名', '手机尾号', '感兴趣项目'],
    ]
    records.forEach((r) => {
      const booth = booths.find((b) => b.id === r.boothId)
      rows.push([
        new Date(r.timestamp).toLocaleString('zh-CN'),
        booth?.name || r.boothId,
        r.visitor.name,
        r.visitor.phoneSuffix,
        r.interestedProjects.join('、'),
      ])
    })

    const csvContent =
      '\uFEFF' +
      rows
        .map((row) =>
          row
            .map((cell) => {
              const s = String(cell ?? '')
              if (s.includes(',') || s.includes('"') || s.includes('\n')) {
                return `"${s.replace(/"/g, '""')}"`
              }
              return s
            })
            .join(',')
        )
        .join('\n')

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute(
      'download',
      `签到记录_${new Date().toISOString().slice(0, 10)}.csv`
    )
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
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
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            flexWrap: 'wrap',
          }}
        >
          <div className="current-time">{formatTime(currentTime)}</div>
        </div>
      </div>

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '20px',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        <div className="range-filter">
          {RANGE_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              className={`range-btn ${timeRange === opt.value ? 'active' : ''}`}
              onClick={() => setTimeRange(opt.value)}
            >
              {opt.label}
            </button>
          ))}
        </div>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <button
            className={`sound-btn ${soundEnabled ? 'on' : ''}`}
            onClick={() => setSoundEnabled(!soundEnabled)}
            title={soundEnabled ? '关闭提示音' : '开启高峰提示音'}
          >
            {soundEnabled ? '🔊 提示音开' : '🔇 提示音关'}
          </button>
          <button className="btn-secondary" onClick={exportCSV}>
            📥 导出 CSV
          </button>
        </div>
      </div>

      <StatsCards
        boothStats={boothStats}
        projectStats={projectStats}
        booths={booths}
        peakBooths={peakBooths}
        todayTotal={todayTotal}
      />

      <div className="two-col-grid">
        <div className={`card ${peakBooths.length > 0 ? 'peak-card' : ''}`}>
          <h2 className="card-title">
            展位人流排行
            {peakBooths.length > 0 && (
              <span
                className="peak-indicator"
                style={{ marginLeft: '8px' }}
              >
                ⚠️ 有 {peakBooths.length} 个展位人流高峰
              </span>
            )}
          </h2>
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
                        <span className="peak-tag peak-pulse">
                          🔥人流高峰
                        </span>
                      )}
                    </span>
                    <span className="progress-count">{booth.count}人</span>
                  </div>
                  <div className="progress-bar">
                    <div
                      className={`progress-fill ${booth.isPeak ? 'peak-fill' : ''}`}
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
