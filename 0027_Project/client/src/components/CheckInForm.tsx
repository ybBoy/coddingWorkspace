import { useState, useEffect, useRef } from 'react'
import { socket } from '../core/socket'
import {
  eventBus,
  EVENT_CHECKIN,
  EVENT_CHECKIN_ERROR,
} from '../core/EventBus'

interface CheckInFormProps {
  boothId: string
}

const PROJECT_OPTIONS = [
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
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const pendingRef = useRef(false)
  const successTimerRef = useRef<any>(null)

  useEffect(() => {
    const handleCheckInSuccess = () => {
      if (pendingRef.current) {
        pendingRef.current = false
        setSubmitting(false)
        setSuccess('签到成功！')
        setName('')
        setPhoneSuffix('')
        setSelectedProjects([])
        if (successTimerRef.current) clearTimeout(successTimerRef.current)
        successTimerRef.current = setTimeout(() => setSuccess(''), 3000)
      }
    }

    const handleCheckInError = (payload: any) => {
      if (pendingRef.current) {
        pendingRef.current = false
        setSubmitting(false)
        setError(payload?.message || '签到失败，请重试')
      }
    }

    const unsub1 = eventBus.on(EVENT_CHECKIN, handleCheckInSuccess)
    const unsub2 = eventBus.on(EVENT_CHECKIN_ERROR, handleCheckInError)

    return () => {
      unsub1()
      unsub2()
      if (successTimerRef.current) clearTimeout(successTimerRef.current)
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

    pendingRef.current = true
    setSubmitting(true)

    socket.sendCheckIn({
      boothId,
      visitor: {
        name: name.trim(),
        phoneSuffix,
      },
      interestedProjects: [...selectedProjects],
    })

    setTimeout(() => {
      if (pendingRef.current) {
        pendingRef.current = false
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
          <div className="checkbox-group">
            {PROJECT_OPTIONS.map((project) => (
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
        </div>

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? '提交中...' : '提交签到'}
        </button>
      </form>
    </div>
  )
}

export default CheckInForm
