import { useState, useEffect } from 'react'
import type { CheckInRecord, Booth } from '../core/socket'
import { eventBus, EVENT_FILTER_CHANGE } from '../core/EventBus'

interface RecentListProps {
  records: CheckInRecord[]
  booths: Booth[]
  filterBoothId?: string
}

function RecentList({ records, booths, filterBoothId }: RecentListProps) {
  const [localFilter, setLocalFilter] = useState<string>(
    filterBoothId || 'all'
  )

  useEffect(() => {
    if (filterBoothId !== undefined) {
      setLocalFilter(filterBoothId)
    }
  }, [filterBoothId])

  const getBoothName = (boothId: string): string => {
    const booth = booths.find((b) => b.id === boothId)
    return booth ? booth.name : '未知展位'
  }

  const maskName = (name: string): string => {
    if (!name) return '***'
    if (name.length === 1) return name + '**'
    return name.charAt(0) + '*'.repeat(name.length - 1)
  }

  const formatTime = (ts: number): string => {
    return new Date(ts).toLocaleTimeString('zh-CN')
  }

  const handleFilterChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value
    setLocalFilter(value)
    eventBus.emit(EVENT_FILTER_CHANGE, value === 'all' ? undefined : value)
  }

  const filteredRecords =
    localFilter === 'all'
      ? records
      : records.filter((r) => r.boothId === localFilter)

  const sortedRecords = [...filteredRecords]
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 10)

  return (
    <div className="card">
      <div className="filter-bar">
        <h2 className="card-title">最近签到记录</h2>
        <select
          className="form-select filter-select"
          value={localFilter}
          onChange={handleFilterChange}
        >
          <option value="all">全部展位</option>
          {booths.map((booth) => (
            <option key={booth.id} value={booth.id}>
              {booth.name}
            </option>
          ))}
        </select>
      </div>

      <div className="records-list">
        {sortedRecords.length === 0 ? (
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
          sortedRecords.map((record) => (
            <div key={record.id} className="record-item">
              <div className="record-left">
                <div className="record-name">{maskName(record.visitor.name)}</div>
                <div className="record-meta">
                  <span>手机尾号: {record.visitor.phoneSuffix}</span>
                  <span>展位: {getBoothName(record.boothId)}</span>
                </div>
                <div className="record-projects">
                  {record.interestedProjects.map((project) => (
                    <span key={project} className="project-tag">
                      {project}
                    </span>
                  ))}
                </div>
              </div>
              <div className="record-time">{formatTime(record.timestamp)}</div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

export default RecentList
