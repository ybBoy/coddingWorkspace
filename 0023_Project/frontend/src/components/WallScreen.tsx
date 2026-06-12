import React, { useEffect, useRef, useState, useCallback } from 'react';
import { eventBus } from '../utils/EventBus';
import { DanmakuMessage, Settings } from '../types';

interface DanmakuItem extends DanmakuMessage {
  top: number;
  duration: number;
  track: number;
  instanceKey: string;
}

const MAX_DANMAKU = 30;

export const WallScreen: React.FC = () => {
  const [danmakus, setDanmakus] = useState<DanmakuItem[]>([]);
  const [pinned, setPinned] = useState<DanmakuMessage[]>([]);
  const [settings, setSettings] = useState<Settings | null>(null);
  const [playbackPaused, setPlaybackPaused] = useState(false);
  const [showQR, setShowQR] = useState(false);
  const trackAvailableAt = useRef<number[]>([]);
  const containerRef = useRef<HTMLDivElement>(null);
  const instanceCounter = useRef(0);
  const removeTimers = useRef<Map<string, number>>(new Map());
  const historyLoaded = useRef(false);
  const pausedQueue = useRef<DanmakuMessage[]>([]);

  const getTrackCount = () => settings?.trackCount || 12;
  const getSpeedMin = () => (settings?.speedMin || 8);
  const getSpeedMax = () => (settings?.speedMax || 14);
  const getFontSize = () => (settings?.fontSize || 28);

  const findAvailableTrack = useCallback((): number => {
    const tc = getTrackCount();
    if (trackAvailableAt.current.length !== tc) {
      trackAvailableAt.current = new Array(tc).fill(0);
    }
    const now = Date.now();
    let best = 0;
    let earliest = trackAvailableAt.current[0];
    for (let i = 0; i < tc; i++) {
      if (trackAvailableAt.current[i] <= now) return i;
      if (trackAvailableAt.current[i] < earliest) { earliest = trackAvailableAt.current[i]; best = i; }
    }
    return best;
  }, [settings]);

  const removeDanmaku = useCallback((key: string) => {
    setDanmakus(prev => prev.filter(d => d.instanceKey !== key));
    removeTimers.current.delete(key);
  }, []);

  const addDanmaku = useCallback((msg: DanmakuMessage) => {
    instanceCounter.current += 1;
    const instanceKey = msg.id + '-' + instanceCounter.current;
    const tc = getTrackCount();
    const track = findAvailableTrack();
    const speedMin = getSpeedMin();
    const speedMax = getSpeedMax();
    const duration = speedMin + Math.random() * (speedMax - speedMin);
    const containerHeight = containerRef.current?.clientHeight || window.innerHeight;
    const trackHeight = containerHeight / tc;
    const top = track * trackHeight + Math.random() * (trackHeight * 0.3);
    if (trackAvailableAt.current.length === tc) {
      trackAvailableAt.current[track] = Date.now() + duration * 1000 * 0.35;
    }

    const item: DanmakuItem = { ...msg, top, duration, track, instanceKey };
    setDanmakus(prev => {
      let next = [...prev, item];
      if (next.length > MAX_DANMAKU) {
        next.slice(0, next.length - MAX_DANMAKU).forEach(d => {
          const t = removeTimers.current.get(d.instanceKey);
          if (t) { clearTimeout(t); removeTimers.current.delete(d.instanceKey); }
        });
        next = next.slice(next.length - MAX_DANMAKU);
      }
      return next;
    });
    const timer = window.setTimeout(() => removeDanmaku(instanceKey), duration * 1000 + 500);
    removeTimers.current.set(instanceKey, timer);
  }, [findAvailableTrack, removeDanmaku, settings]);

  useEffect(() => {
    if (!historyLoaded.current) {
      historyLoaded.current = true;
      eventBus.emit('GET_HISTORY');
    }

    const unsub1 = eventBus.on('NEW_MESSAGE', (msg: DanmakuMessage) => {
      if (playbackPaused) { pausedQueue.current.push(msg); return; }
      addDanmaku(msg);
    });
    const unsub2 = eventBus.on('CLEAR_SCREEN', () => {
      setDanmakus([]);
      setPinned([]);
      trackAvailableAt.current = new Array(getTrackCount()).fill(0);
      removeTimers.current.forEach(t => clearTimeout(t));
      removeTimers.current.clear();
      pausedQueue.current = [];
    });
    const unsub3 = eventBus.on('SETTING_UPDATED', (data: any) => setSettings(data as Settings));
    const unsub4 = eventBus.on('HISTORY_MESSAGES', (msgs: DanmakuMessage[]) => {
      if (msgs && msgs.length > 0) msgs.forEach((m, i) => setTimeout(() => addDanmaku(m), i * 200));
    });
    const unsub5 = eventBus.on('PIN_UPDATED', (msg: DanmakuMessage) => {
      setPinned(prev => msg.pinned ? [...prev.filter(p => p.id !== msg.id), msg] : prev.filter(p => p.id !== msg.id));
    });
    const unsub6 = eventBus.on('PLAYBACK_STATE', (data: any) => {
      const paused = !!data?.playbackPaused;
      setPlaybackPaused(paused);
      if (!paused && pausedQueue.current.length > 0) {
        pausedQueue.current.forEach(m => addDanmaku(m));
        pausedQueue.current = [];
      }
    });
    return () => { unsub1(); unsub2(); unsub3(); unsub4(); unsub5(); unsub6();
      removeTimers.current.forEach(t => clearTimeout(t)); removeTimers.current.clear();
    };
  }, [addDanmaku, playbackPaused]);

  const fontSize = getFontSize();
  const qrUrl = window.location.origin;

  return (
    <div className="wall-screen" ref={containerRef}>
      <div className="wall-header">
        <div className="wall-logo">{settings?.eventTitle || 'LIVE DANMAKU'}</div>
        <div className="wall-controls">
          <button className="wall-ctrl-btn" onClick={() => setShowQR(!showQR)} title="QR Code">QR</button>
          <button className="wall-ctrl-btn" onClick={() => eventBus.emit('TOGGLE_PLAYBACK', { data: { paused: !playbackPaused } })}>
            {playbackPaused ? 'Resume' : 'Pause'}
          </button>
          <span className="wall-stats-text">{'Danmaku: ' + danmakus.length + '/' + MAX_DANMAKU}</span>
          <span className={settings?.sendingEnabled !== false ? 'status-on' : 'status-off'}>
            {settings?.sendingEnabled !== false ? 'Sending ON' : 'Sending OFF'}
          </span>
        </div>
      </div>

      {showQR && (
        <div className="qr-overlay" onClick={() => setShowQR(false)}>
          <div className="qr-card" onClick={e => e.stopPropagation()}>
            <div className="qr-placeholder">
              <p>Scan to join</p>
              <p className="qr-url">{qrUrl}</p>
            </div>
            <button className="qr-close" onClick={() => setShowQR(false)}>Close</button>
          </div>
        </div>
      )}

      {pinned.length > 0 && (
        <div className="pinned-bar">
          {pinned.map(msg => (
            <div key={msg.id} className="pinned-item" style={{ color: msg.color }}>
              <span className="pinned-badge">PINNED</span>
              <span className="pinned-nick">{msg.nickname}:</span>
              <span className="pinned-content">{msg.content}</span>
            </div>
          ))}
        </div>
      )}

      <div className={'danmaku-container' + (playbackPaused ? ' paused' : '')}>
        {danmakus.map(d => (
          <div key={d.instanceKey}
            className={'danmaku-item' + (d.sensitive ? ' sensitive' : '')}
            style={{ top: d.top, color: d.color, animationDuration: d.duration + 's', fontSize: fontSize + 'px' }}>
            <span className="danmaku-nickname">{d.nickname}:</span>
            <span className="danmaku-content">{d.content}</span>
          </div>
        ))}
      </div>

      {danmakus.length === 0 && pinned.length === 0 && (
        <div className="empty-wall">
          <p>No danmaku yet</p>
          <p className="sub">Waiting for the first message...</p>
        </div>
      )}
    </div>
  );
};
