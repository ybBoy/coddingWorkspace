import { useState, useEffect, useRef, useCallback } from 'react'
import StatsCards from '../components/StatsCards'
import RecentList from '../components/RecentList'
import ConfigModal from '../components/ConfigModal'
import type { Booth, CheckInRecord, SnapshotData, RangeStatsData } from '../core/socket'
import { socket } from '../core/socket'
import {
  eventBus,
  EVENT_STATS_REFRESH,
  EVENT_RECORDS_UPDATE,
  EVENT_FILTER_CHANGE,
  EVENT_RANGE_STATS,
  EVENT_PEAK_ALERT,
  EVENT_EXPORT_RECORDS,
  EVENT_BACKUP_DATA,
  EVENT_CLEAR_DATA_ACK,
} from '../core/EventBus'

type TimeRange = '10min' | 'today' | 'all'
type CarouselMode = 'none' | 'booth' | 'project'

const RANGE_OPTIONS: { value: TimeRange; label: string }[] = [
  { value: '10min', label: '最近10分钟' },
  { value: 'today', label: '今天' },
  { value: 'all', label: '全部' },
]

const CAROUSEL_OPTIONS: { value: CarouselMode; label: string }[] = [
  { value: 'none', label: '关闭轮播' },
  { value: 'booth', label: '按展位轮播' },
  { value: 'project', label: '按项目轮播' },
]

function DashboardPage() {
  const [currentTime, setCurrentTime] = useState(new Date())
  const [booths, setBooths] = useState<Booth[]>([])
  const [allBooths, setAllBooths] = useState<Booth[]>([])
  const [projects, setProjects] = useState<string[]>([])
  const [records, setRecords] = useState<CheckInRecord[]>([])
  const [boothStats, setBoothStats] = useState<Record<string, number>>({})
  const [projectStats, setProjectStats] = useState<Record<string, number>>({})
  const [peakBooths, setPeakBooths] = useState<string[]>([])
  const [filterBoothId, setFilterBoothId] = useState<string | undefined>(undefined)
  const [timeRange, setTimeRange] = useState<TimeRange>('all')
  const [todayTotal, setTodayTotal] = useState<number>(0)
  const [soundEnabled, setSoundEnabled] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [configOpen, setConfigOpen] = useState(false)
  const [fullscreenMode, setFullscreenMode] = useState(false)
  const [carouselMode, setCarouselMode] = useState<CarouselMode>('none')
  const [carouselIdx, setCarouselIdx] = useState(0)
  const [backingUp, setBackingUp] = useState(false)
  const [clearing, setClearing] = useState(false)

  const prevPeakRef = useRef<Set<string>>(new Set())
  const audioCtxRef = useRef<AudioContext | null>(null)
  const exportCallbackRef = useRef<((records: CheckInRecord[]) => void) | null>(null)
  const timeRangeRef = useRef<TimeRange>('all')
  const carouselTimerRef = useRef<number | null>(null)
  const backupCallbackRef = useRef<
    ((payload: { backupJson: string; filename: string }) => void
  ) | null>(null)
  const clearCallbackRef = useRef<(() => void) | null>(null)

  useEffect(() => {
    timeRangeRef.current = timeRange
  }, [timeRange])

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
    if (carouselMode === 'none') {
      setFilterBoothId(undefined)
      setCarouselIdx(0)
      if (carouselTimerRef.current) {
        clearInterval(carouselTimerRef.current)
        carouselTimerRef.current = null
      }
      return
    }

    const getItems = () => {
      if (carouselMode === 'booth') {
        return [...booths.filter((b) => !b.disabled).map((b) => b.id)]
      } else {
        return [...projects]
      }
    }

    const advance = () => {
      const items = getItems()
      if (items.length === 0) return
      setCarouselIdx((prev) => {
        const next = (prev + 1) % items.length
        const item = items[next]
        if (carouselMode === 'booth') {
          setFilterBoothId(item)
        }
        return next
      })
    }

    const items = getItems()
    if (items.length > 0) {
      const first = items[0]
      if (carouselMode === 'booth') {
        setFilterBoothId(first)
      }
    }
    setCarouselIdx(0)

    carouselTimerRef.current = window.setInterval(advance, 8000)

    return () => {
      if (carouselTimerRef.current) {
        clearInterval(carouselTimerRef.current)
        carouselTimerRef.current = null
      }
    }
  }, [carouselMode, booths, projects])

  useEffect(() => {
    const handleStatsRefresh = (data: SnapshotData) => {
      if (!data) return
      if (data.booths) setBooths(data.booths)
      if (data.allBooths) setAllBooths(data.allBooths)
      if (data.projects) setProjects(data.projects)
      if (data.peakBooths) {
        setPeakBooths(data.peakBooths)
        detectNewPeaks(data.peakBooths)
      }
      if (data.todayTotal !== undefined) setTodayTotal(data.todayTotal)

      const currentRange = timeRangeRef.current
      if (currentRange === 'all') {
        if (data.boothStats) setBoothStats(data.boothStats)
        if (data.projectStats) setProjectStats(data.projectStats)
        if (data.records) setRecords(data.records)
      } else if (currentRange === 'today') {
        if (data.todayBoothStats) setBoothStats(data.todayBoothStats)
        if (data.todayProjectStats) setProjectStats(data.todayProjectStats)
        if (data.records) {
          const todayStart = new Date()
          todayStart.setHours(0, 0, 0, 0)
          setRecords(data.records.filter((r) => r.timestamp >= todayStart.getTime()))
        }
        socket.requestRecordsByRange(currentRange)
      } else {
        socket.requestRecordsByRange(currentRange)
      }
    }

    const handleRecordsUpdate = (newRecords: CheckInRecord[]) => {
      if (Array.isArray(newRecords) && timeRangeRef.current === 'all') {
        setRecords(newRecords)
      }
    }

    const handleFilterChange = (boothId?: string) => {
      setFilterBoothId(boothId)
    }

    const handleRangeStats = (data: RangeStatsData) => {
      if (data.range === timeRangeRef.current) {
        if (data.records) setRecords(data.records)
        if (data.boothStats) setBoothStats(data.boothStats)
        if (data.projectStats) setProjectStats(data.projectStats)
      }
    }

    const handleExportRecords = (allRecords: CheckInRecord[]) => {
      if (exportCallbackRef.current && Array.isArray(allRecords)) {
        const cb = exportCallbackRef.current
        exportCallbackRef.current = null
        setExporting(false)
        cb(allRecords)
      }
    }

    const handleBackupData = (payload: { backupJson: string; filename: string }) => {
      if (backupCallbackRef.current) {
        const cb = backupCallbackRef.current
        backupCallbackRef.current = null
        setBackingUp(false)
        cb(payload)
      }
    }

    const handleClearAck = () => {
      if (clearCallbackRef.current) {
        const cb = clearCallbackRef.current
        clearCallbackRef.current = null
        setClearing(false)
        cb()
      }
    }

    const unsub1 = eventBus.on(EVENT_STATS_REFRESH, handleStatsRefresh)
    const unsub2 = eventBus.on(EVENT_RECORDS_UPDATE, handleRecordsUpdate)
    const unsub3 = eventBus.on(EVENT_FILTER_CHANGE, handleFilterChange)
    const unsub4 = eventBus.on(EVENT_RANGE_STATS, handleRangeStats)
    const unsub5 = eventBus.on(EVENT_EXPORT_RECORDS, handleExportRecords)
    const unsub6 = eventBus.on(EVENT_BACKUP_DATA, handleBackupData)
    const unsub7 = eventBus.on(EVENT_CLEAR_DATA_ACK, handleClearAck)

    socket.connect()
    socket.requestStats()

    return () => {
      unsub1()
      unsub2()
      unsub3()
      unsub4()
      unsub5()
      unsub6()
      unsub7()
    }
  }, [detectNewPeaks])

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

  const doExport = (exportRecords: CheckInRecord[]) => {
    const boothMap = new Map(allBooths.map((b) => [b.id, b.name]))
    const rows = [
      ['签到时间', '展位', '姓名', '手机尾号', '感兴趣项目'],
    ]
    exportRecords.forEach((r) => {
      rows.push([
        new Date(r.timestamp).toLocaleString('zh-CN'),
        boothMap.get(r.boothId) || r.boothId,
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

  const exportCSV = () => {
    if (exporting) return
    setExporting(true)
    const timeoutId = setTimeout(() => {
      if (exportCallbackRef.current === doExport) {
        exportCallbackRef.current = null
        setExporting(false)
      }
    }, 10000)
    exportCallbackRef.current = (records: CheckInRecord[]) => {
      clearTimeout(timeoutId)
      doExport(records)
    }
    socket.requestExportRecords()
  }

  const handleBackup = () => {
    if (backingUp) return
    if (!window.confirm('确定要备份当前所有数据吗？备份完成后可继续使用清场功能。')) {
      return
    }
    setBackingUp(true)
    const timeoutId = setTimeout(() => {
      if (backupCallbackRef.current === handleBackupPayload) {
        backupCallbackRef.current = null
        setBackingUp(false)
        alert('备份请求超时，请重试')
      }
    }, 10000)
    const handleBackupPayload = (payload: { backupJson: string; filename: string }) => {
      clearTimeout(timeoutId)
      const blob = new Blob([payload.backupJson], { type: 'application/json' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)
      link.setAttribute('href', url)
      link.setAttribute('download', payload.filename)
      link.style.visibility = 'hidden'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    }
    backupCallbackRef.current = handleBackupPayload
    socket.requestBackup()
  }

  const handleClearAll = () => {
    if (clearing) return
    if (
      !window.confirm(
        '⚠️ 危险操作：确定要清空所有签到数据吗？\n\n请先确认已完成数据备份！此操作会清空内存中的所有签到记录并保存到本地 JSON，展位和项目配置会保留。'
      )
    ) {
      return
    }
    if (
      !window.confirm(
        '再次确认：所有签到数据将被清空且无法从系统恢复（如有备份可从备份文件恢复），确定继续？'
      )
    ) {
      return
    }
    setClearing(true)
    const timeoutId = setTimeout(() => {
      if (clearCallbackRef.current === handleClearDone) {
        clearCallbackRef.current = null
        setClearing(false)
        alert('清场请求超时，请重试')
      }
    }, 10000)
    const handleClearDone = () => {
      clearTimeout(timeoutId)
      alert('清场完成！所有签到记录已清空。')
    }
    clearCallbackRef.current = handleClearDone
    socket.requestClearAllData()
  }

  const toggleFullscreen = async () => {
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen()
        setFullscreenMode(true)
      } else {
        await document.exitFullscreen()
        setFullscreenMode(false)
      }
    } catch (e) {
      console.warn('Fullscreen not supported:', e)
      setFullscreenMode(!fullscreenMode)
    }
  }

  useEffect(() => {
    const onFsChange = () => {
      setFullscreenMode(!!document.fullscreenElement)
    }
    document.addEventListener('fullscreenchange', onFsChange)
    return () => {
      document.removeEventListener('fullscreenchange', onFsChange)
    }
  }, [])

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

  const containerClass = fullscreenMode ? 'dashboard-fullscreen' : ''

  const carouselLabel =
    carouselMode === 'booth'
      ? booths[carouselIdx]?.name || ''
      : carouselMode === 'project'
      ? projects[carouselIdx] || ''
      : ''

  return (
    <div className={containerClass}>
      {!fullscreenMode && (
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
      )}

      {!fullscreenMode && (
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
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
            <button
              className={`sound-btn ${soundEnabled ? 'on' : ''}`}
              onClick={() => setSoundEnabled(!soundEnabled)}
              title={soundEnabled ? '关闭提示音' : '开启高峰提示音'}
            >
              {soundEnabled ? '🔊 提示音开' : '🔇 提示音关'}
            </button>
            <button
              className="btn-secondary"
              onClick={() => setConfigOpen(true)}
            >
              ⚙️ 配置管理
            </button>
            <button
              className="btn-secondary"
              onClick={handleBackup}
              disabled={backingUp}
            >
              {backingUp ? '⏳ 备份中...' : '💾 数据备份'}
            </button>
            <button
              className="btn-secondary"
              onClick={handleClearAll}
              disabled={clearing}
              style={{ color: '#dc2626', borderColor: '#fecaca' }}
            >
              {clearing ? '⏳ 清场中...' : '🗑️ 清场模式'}
            </button>
            <button className="btn-secondary" onClick={exportCSV} disabled={exporting}>
              {exporting ? '⏳ 导出中...' : '📥 导出 CSV'}
            </button>
            <button className="btn-secondary" onClick={toggleFullscreen}>
              🖥️ 大屏模式
            </button>
          </div>
        </div>
      )}

      {fullscreenMode && (
        <div className="fs-toolbar">
          <div className="fs-title">
            📊 实时人流看板 · {formatTime(currentTime)}
            {carouselMode !== 'none' && (
              <span style={{ marginLeft: '16px', fontSize: '16px', color: '#94a3b8' }}>
                正在轮播：{carouselLabel}
              </span>
            )}
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <div className="range-filter">
              {CAROUSEL_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  className={`range-btn ${carouselMode === opt.value ? 'active' : ''}`}
                  onClick={() => setCarouselMode(opt.value)}
                >
                  {opt.label}
                </button>
              ))}
            </div>
            <button
              className={`sound-btn ${soundEnabled ? 'on' : ''}`}
              onClick={() => setSoundEnabled(!soundEnabled)}
            >
              {soundEnabled ? '🔊' : '🔇'}
            </button>
            <button className="btn-secondary" onClick={toggleFullscreen}>
              退出大屏
            </button>
          </div>
        </div>
      )}

      <StatsCards
        boothStats={boothStats}
        projectStats={projectStats}
        booths={booths}
        peakBooths={peakBooths}
        todayTotal={todayTotal}
        timeRange={timeRange}
      />

      <div className="two-col-grid">
        <div className={`card ${peakBooths.length > 0 ? 'peak-card' : ''}`}>
          <h2 className="card-title">
            展位人流排行
            {peakBooths.length > 0 && (
              <span className="peak-indicator" style={{ marginLeft: '8px' }}>
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
                        <span className="peak-tag peak-pulse">🔥人流高峰</span>
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

      <ConfigModal
        open={configOpen}
        onClose={() => setConfigOpen(false)}
        allBooths={allBooths}
        projects={projects}
      />
    </div>
  )
}

export default DashboardPage
