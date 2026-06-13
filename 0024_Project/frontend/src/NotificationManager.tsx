import { useEffect, useState } from 'react'
import { EventBus, EVT } from './EventBus'
import type { Order } from './types'

/**
 * 新订单提醒（声音 + 桌面通知）
 *
 * 职责：
 *   - 订阅 NEW_ORDER_ARRIVED 事件
 *   - 声音提醒：用 Web Audio API 合成一段短促提示音（避免引入音频文件）
 *   - 桌面通知：用浏览器 Notification API（需要用户授权）
 *   - 提供开关按钮，用户可以手动切换声音/桌面通知
 *
 * 挂在 App 顶部，组件不渲染可见 UI，仅渲染一个设置按钮区域
 */
interface Props {
  /** 渲染模式: 'icon' 只显示图标按钮, 'full' 显示完整开关行 */
  mode?: 'icon' | 'full'
}

// 用 Web Audio 生成提示音，避免外部资源
let audioCtx: AudioContext | null = null
function playDing() {
  try {
    if (!audioCtx) {
      const Ctx = (window.AudioContext || (window as any).webkitAudioContext)
      if (!Ctx) return
      audioCtx = new Ctx()
    }
    const ctx = audioCtx
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'sine'
    osc.frequency.setValueAtTime(880, ctx.currentTime)
    osc.frequency.exponentialRampToValueAtTime(660, ctx.currentTime + 0.15)
    gain.gain.setValueAtTime(0.001, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.3, ctx.currentTime + 0.02)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35)
    osc.connect(gain).connect(ctx.destination)
    osc.start()
    osc.stop(ctx.currentTime + 0.4)
  } catch (e) {
    console.warn('[Notify] audio error', e)
  }
}

// 桌面通知
function showDesktop(order: Order) {
  if (!('Notification' in window)) return
  if (Notification.permission !== 'granted') return
  const title = `🍳 新订单 - ${order.tableNo} 桌`
  const body = order.dishes.map((d) => `${d.name} ×${d.quantity}`).join('、')
  try {
    new Notification(title, { body, icon: undefined })
  } catch (e) {
    console.warn('[Notify] desktop notify error', e)
  }
}

export default function NotificationManager({ mode = 'icon' }: Props) {
  const [soundOn, setSoundOn] = useState<boolean>(() => {
    return localStorage.getItem('notify_sound') !== '0'
  })
  const [desktopOn, setDesktopOn] = useState<boolean>(() => {
    return localStorage.getItem('notify_desktop') === '1'
  })
  const [permission, setPermission] = useState<NotificationPermission>(
    () => ('Notification' in window ? Notification.permission : 'denied'),
  )

  // 持久化配置
  useEffect(() => {
    localStorage.setItem('notify_sound', soundOn ? '1' : '0')
  }, [soundOn])
  useEffect(() => {
    localStorage.setItem('notify_desktop', desktopOn ? '1' : '0')
  }, [desktopOn])

  // 订阅新订单事件
  useEffect(() => {
    const unSub = EventBus.on<Order>(EVT.NEW_ORDER_ARRIVED, (order) => {
      if (!order) return
      if (soundOn) playDing()
      if (desktopOn) showDesktop(order)
    })
    return unSub
  }, [soundOn, desktopOn])

  const requestDesktop = async () => {
    if (!('Notification' in window)) return
    const p = await Notification.requestPermission()
    setPermission(p)
    if (p === 'granted') setDesktopOn(true)
    else setDesktopOn(false)
  }

  const toggleSound = () => setSoundOn((v) => !v)
  const toggleDesktop = () => {
    if (permission === 'granted') {
      setDesktopOn((v) => !v)
    } else {
      requestDesktop()
    }
  }

  if (mode === 'icon') {
    return (
      <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
        <button
          onClick={toggleSound}
          title={soundOn ? '关闭声音提醒' : '开启声音提醒'}
          style={{
            background: soundOn ? 'rgba(255,255,255,.25)' : 'rgba(0,0,0,.15)',
            color: '#fff',
            padding: '6px 10px',
            fontSize: 14,
            borderRadius: 6,
          }}
        >
          {soundOn ? '🔔' : '🔕'}
        </button>
        <button
          onClick={toggleDesktop}
          title={desktopOn ? '关闭桌面通知' : '开启桌面通知'}
          style={{
            background: desktopOn ? 'rgba(255,255,255,.25)' : 'rgba(0,0,0,.15)',
            color: '#fff',
            padding: '6px 10px',
            fontSize: 14,
            borderRadius: 6,
          }}
        >
          💬
        </button>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
      <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}>
        <input type="checkbox" checked={soundOn} onChange={toggleSound} />
        <span>🔔 声音提醒</span>
      </label>
      <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}>
        <input
          type="checkbox"
          checked={desktopOn && permission === 'granted'}
          onChange={toggleDesktop}
        />
        <span>💬 桌面通知</span>
        {permission === 'denied' && (
          <span style={{ fontSize: 12, color: '#c62828' }}>（已被浏览器阻止）</span>
        )}
      </label>
    </div>
  )
}
