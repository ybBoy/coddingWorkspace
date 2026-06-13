import { useState, useEffect, useRef } from 'react'
import QRCode from 'qrcode'
import CheckInForm from '../components/CheckInForm'
import type { Booth, CheckInRecord, SnapshotData } from '../core/socket'
import { socket } from '../core/socket'
import {
  eventBus,
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
  const [records, setRecords] = useState<CheckInRecord[]>([])
  const [todayBoothStats, setTodayBoothStats] = useState<Record<string, number>>({})
  const [showQR, setShowQR] = useState(false)
  const [qrDataUrl, setQrDataUrl] = useState('')
  const qrGeneratedRef = useRef<Set<string>>(new Set())

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const boothFromUrl = params.get('booth')
    if (boothFromUrl) {
      setSelectedBoothId(boothFromUrl)
    }
  }, [])

  useEffect(() => {
    const handleStatsRefresh = (data: SnapshotData) => {
      if (data.booths && data.booths.length > 0) {
        setBooths(data.booths)
      }
      if (data.todayBoothStats) {
        setTodayBoothStats(data.todayBoothStats)
      }
    }

    const handleRecordsUpdate = (newRecords: CheckInRecord[]) => {
      if (Array.isArray(newRecords)) {
        setRecords(newRecords)
      }
    }

    const unsub1 = eventBus.on(EVENT_STATS_REFRESH, handleStatsRefresh)
    const unsub2 = eventBus.on(EVENT_RECORDS_UPDATE, handleRecordsUpdate)

    return () => {
      unsub1()
      unsub2()
    }
  }, [selectedBoothId])

  useEffect(() => {
    socket.connect()
    socket.requestStats()
  }, [])

  useEffect(() => {
    if (showQR && selectedBoothId) {
      if (qrGeneratedRef.current.has(selectedBoothId) && qrDataUrl) {
        return
      }
      const url = `${window.location.origin}${window.location.pathname}?booth=${selectedBoothId}`
      QRCode.toDataURL(url, { width: 200, margin: 2 })
        .then((dataUrl) => {
          setQrDataUrl(dataUrl)
          qrGeneratedRef.current.add(selectedBoothId)
        })
        .catch((err) => {
          console.error('QRCode generation failed:', err)
        })
    }
  }, [showQR, selectedBoothId, qrDataUrl])

  const todayCount = todayBoothStats[selectedBoothId] || 0

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
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <h1 className="page-title">展位签到系统</h1>
          <button
            className="btn-secondary"
            onClick={() => setShowQR(!showQR)}
          >
            {showQR ? '隐藏二维码' : '显示展位二维码'}
          </button>
        </div>
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

        {showQR && qrDataUrl && (
          <div className="qr-popup">
            <div className="qr-card">
              <h3 style={{ marginBottom: '12px' }}>{selectedBoothName} 签到二维码</h3>
              <img
                src={qrDataUrl}
                alt="展位签到二维码"
                style={{ width: '200px', height: '200px' }}
              />
              <p style={{ marginTop: '12px', fontSize: '13px', color: 'var(--text-secondary)' }}>
                扫码后直接进入 {selectedBoothName} 签到页
              </p>
            </div>
          </div>
        )}
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
