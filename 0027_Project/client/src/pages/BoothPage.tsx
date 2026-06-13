import { useState, useEffect } from 'react'
import CheckInForm from '../components/CheckInForm'
import type { Booth, CheckInRecord, SnapshotData } from '../core/socket'
import { socket } from '../core/socket'
import {
  eventBus,
  EVENT_CHECKIN,
  EVENT_STATS_REFRESH,
  EVENT_RECORDS_UPDATE,
} from '../core/EventBus'

const DEFAULT_BOOTHS: Booth[] = [
  { id: 'B001', name: '人工智能展', description: '展示最新的人工智能技术与应用' },
  { id: 'B002', name: '智能硬件展', description: '展示创新的智能硬件产品' },
  { id: 'B003', name: '数字文创展', description: '展示数字文化创意产业成果' },
]

function BoothPage() {
  const [selectedBoothId, setSelectedBoothId] = useState<string>(
    DEFAULT_BOOTHS[0].id
  )
  const [booths, setBooths] = useState<Booth[]>(DEFAULT_BOOTHS)
  const [boothStats, setBoothStats] = useState<Record<string, number>>({})
  const [records, setRecords] = useState<CheckInRecord[]>([])
  const [todayCount, setTodayCount] = useState(0)

  useEffect(() => {
    const handleStatsRefresh = (data: SnapshotData) => {
      if (data.booths && data.booths.length > 0) {
        setBooths(data.booths)
      }
      if (data.boothStats) {
        setBoothStats(data.boothStats)
      }
    }

    const handleRecordsUpdate = (newRecords: CheckInRecord[]) => {
      if (Array.isArray(newRecords)) {
        setRecords(newRecords)
      }
    }

    const handleCheckIn = (record: CheckInRecord) => {
      if (record && record.boothId === selectedBoothId) {
        setTodayCount((prev) => prev + 1)
      }
    }

    const unsub1 = eventBus.on(EVENT_STATS_REFRESH, handleStatsRefresh)
    const unsub2 = eventBus.on(EVENT_RECORDS_UPDATE, handleRecordsUpdate)
    const unsub3 = eventBus.on(EVENT_CHECKIN, handleCheckIn)

    return () => {
      unsub1()
      unsub2()
      unsub3()
    }
  }, [selectedBoothId])

  useEffect(() => {
    const count = boothStats[selectedBoothId] || 0
    setTodayCount(count)
  }, [boothStats, selectedBoothId])

  useEffect(() => {
    socket.connect()
    socket.requestStats()
  }, [])

  const maskName = (name: string): string => {
    if (!name) return '***'
    if (name.length === 1) return name + '**'
    return name.charAt(0) + '*'.repeat(name.length - 1)
  }

  const formatTime = (ts: number): string => {
    return new Date(ts).toLocaleTimeString('zh-CN')
  }

  const boothRecords = records
    .filter((r) => r.boothId === selectedBoothId)
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 10)

  const selectedBoothName =
    booths.find((b) => b.id === selectedBoothId)?.name || '未选择展位'

  return (
    <div>
      <div className="booth-header">
        <h1 className="page-title">展位签到系统</h1>
        <p className="page-subtitle">请先选择您负责的展位，然后为到访的访客完成签到</p>
        <div className="booth-select-wrapper">
          <label className="form-label">选择展位</label>
          <select
            className="form-select"
            value={selectedBoothId}
            onChange={(e) => setSelectedBoothId(e.target.value)}
          >
            {booths.map((booth) => (
              <option key={booth.id} value={booth.id}>
                {booth.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="booth-page-layout">
        <CheckInForm boothId={selectedBoothId} />

        <div>
          <div className="card today-count-card">
            <div className="today-count-value">{todayCount}</div>
            <div className="today-count-label">
              {selectedBoothName} · 今日签到数
            </div>
          </div>

          <div className="card">
            <h2 className="card-title">本展位最近签到</h2>
            <div className="records-list">
              {boothRecords.length === 0 ? (
                <div
                  style={{
                    textAlign: 'center',
                    padding: '32px',
                    color: 'var(--text-secondary)',
                  }}
                >
                  暂无签到记录
                </div>
              ) : (
                boothRecords.map((record) => (
                  <div key={record.id} className="record-item">
                    <div className="record-left">
                      <div className="record-name">
                        {maskName(record.visitor.name)}
                      </div>
                      <div className="record-meta">
                        <span>手机尾号: {record.visitor.phoneSuffix}</span>
                      </div>
                      <div className="record-projects">
                        {record.interestedProjects.map((project) => (
                          <span key={project} className="project-tag">
                            {project}
                          </span>
                        ))}
                      </div>
                    </div>
                    <div className="record-time">
                      {formatTime(record.timestamp)}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default BoothPage
