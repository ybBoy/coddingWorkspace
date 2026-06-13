import { useState, useEffect, useRef } from 'react'
import { socket } from '../core/socket'
import {
  eventBus,
  EVENT_CHECKIN_ACK,
  EVENT_CHECKIN_ERROR,
  EVENT_STATS_REFRESH,
} from '../core/EventBus'
import type { SnapshotData } from '../core/socket'

interface CheckInFormProps {
  boothId: string
}

const DEFAULT_PROJECTS = [
  '智能对话',
  '机器视觉',
  '边缘计算',
  '智能穿戴',
  'AR/VR',
  '数字艺术',
  '互动游戏',
  '开源硬件',
]

function CheckInForm({ boothId }: CheckInFormProps) {
  const [name, setName] = useState('')
  const [phoneSuffix, setPhoneSuffix] = useState('')
  const [selectedProjects, setSelectedProjects] = useState<string[]>([])
  const [projects, setProjects] = useState<string[]>(DEFAULT_PROJECTS)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const pendingRequestIdRef = useRef<string | null>(null)
  const successTimerRef = useRef<any>(null)
  const timeoutTimerRef = useRef<any>(null)

  useEffect(() => {
    const handleCheckInAck = (payload: any) => {
      if (
        pendingRequestIdRef.current &&
        payload?.requestId === pendingRequestIdRef.current
      ) {
        pendingRequestIdRef.current = null
        setSubmitting(false)
        if (timeoutTimerRef.current) {
          clearTimeout(timeoutTimerRef.current)
          timeoutTimerRef.current = null
        }
        setSuccess('签到成功！')
        setName('')
        setPhoneSuffix('')
        setSelectedProjects([])
        if (successTimerRef.current) clearTimeout(successTimerRef.current)
        successTimerRef.current = setTimeout(() => setSuccess(''), 3000)
      }
    }

    const handleCheckInError = (payload: any) => {
      if (
        pendingRequestIdRef.current &&
        payload?.requestId === pendingRequestIdRef.current
      ) {
        pendingRequestIdRef.current = null
        setSubmitting(false)
        if (timeoutTimerRef.current) {
          clearTimeout(timeoutTimerRef.current)
          timeoutTimerRef.current = null
        }
        setError(payload?.message || '签到失败，请重试')
      }
    }

    const handleStatsRefresh = (data: SnapshotData) => {
      if (data.projects && data.projects.length > 0) {
        setProjects(data.projects)
        setSelectedProjects((prev) =>
          prev.filter((p) => data.projects!.includes(p))
        )
      }
    }

    const unsub1 = eventBus.on(EVENT_CHECKIN_ACK, handleCheckInAck)
    const unsub2 = eventBus.on(EVENT_CHECKIN_ERROR, handleCheckInError)
    const unsub3 = eventBus.on(EVENT_STATS_REFRESH, handleStatsRefresh)

    return () => {
      unsub1()
      unsub2()
      unsub3()
      if (successTimerRef.current) clearTimeout(successTimerRef.current)
      if (timeoutTimerRef.current) clearTimeout(timeoutTimerRef.current)
    }
  }, [])

  const handleProjectToggle = (project: string) => {
    setSelectedProjects((prev) =>
      prev.includes(project)
        ? prev.filter((p) => p !== project)
        : [...prev, project]
    )
  }

  const validate = (): boolean => {
    if (!name.trim()) {
      setError('请填写访客姓名')
      return false
    }
    if (!/^\d{4}$/.test(phoneSuffix)) {
      setError('手机号后四位必须是4位数字')
      return false
    }
    if (selectedProjects.length === 0) {
      setError('请至少选择一个感兴趣的项目')
      return false
    }
    return true
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccess('')

    if (!validate()) return

    if (!boothId) {
      setError('请先选择展位')
      return
    }

    const requestId = socket.sendCheckIn({
      boothId,
      visitor: {
        name: name.trim(),
        phoneSuffix,
      },
      interestedProjects: [...selectedProjects],
    })

    pendingRequestIdRef.current = requestId
    setSubmitting(true)

    if (timeoutTimerRef.current) clearTimeout(timeoutTimerRef.current)
    timeoutTimerRef.current = setTimeout(() => {
      if (pendingRequestIdRef.current === requestId) {
        pendingRequestIdRef.current = null
        setSubmitting(false)
        setError('服务器响应超时，请重试')
      }
    }, 8000)
  }

  return (
    <div className="card">
      <h2 className="card-title">访客签到</h2>

      {error && <div className="form-message error">{error}</div>}
      {success && <div className="form-message success">{success}</div>}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">访客姓名</label>
          <input
            type="text"
            className="form-input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="请输入访客姓名"
            maxLength={20}
            disabled={submitting}
          />
        </div>

        <div className="form-group">
          <label className="form-label">手机号后四位</label>
          <input
            type="text"
            className="form-input"
            value={phoneSuffix}
            onChange={(e) => {
              const val = e.target.value.replace(/\D/g, '')
              setPhoneSuffix(val.slice(0, 4))
            }}
            placeholder="请输入手机号后四位"
            maxLength={4}
            pattern="\d{4}"
            inputMode="numeric"
            disabled={submitting}
          />
        </div>

        <div className="form-group">
          <label className="form-label">感兴趣的项目（多选）</label>
          {projects.length === 0 ? (
            <div
              style={{
                padding: '20px',
                textAlign: 'center',
                color: 'var(--text-secondary)',
                fontSize: '14px',
              }}
            >
              暂无可用项目，请先在配置管理中添加项目
            </div>
          ) : (
            <div className="checkbox-group">
              {projects.map((project) => (
                <label
                  key={project}
                  className={`checkbox-item ${
                    selectedProjects.includes(project) ? 'checked' : ''
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={selectedProjects.includes(project)}
                    onChange={() => handleProjectToggle(project)}
                    disabled={submitting}
                  />
                  <span>{project}</span>
                </label>
              ))}
            </div>
          )}
        </div>

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? '提交中...' : '提交签到'}
        </button>
      </form>
    </div>
  )
}

export default CheckInForm
