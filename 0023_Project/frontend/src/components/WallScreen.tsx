import React, { useEffect, useRef, useState, useCallback } from 'react';
import { eventBus } from '../utils/EventBus';
import { DanmakuMessage } from '../types';

interface DanmakuItem extends DanmakuMessage {
  top: number;
  duration: number;
  track: number;
  instanceKey: string;
  createdAt: number;
}

const MAX_DANMAKU = 30;
const TRACK_COUNT = 12;
const MIN_DURATION = 8;
const MAX_DURATION = 14;

export const WallScreen: React.FC = () => {
  const [danmakus, setDanmakus] = useState<DanmakuItem[]>([]);
  const [sendingEnabled, setSendingEnabled] = useState(true);
  const trackAvailableAt = useRef<number[]>(new Array(TRACK_COUNT).fill(0));
  const containerRef = useRef<HTMLDivElement>(null);
  const instanceCounter = useRef(0);
  const removeTimers = useRef<Map<string, number>>(new Map());

  const findAvailableTrack = useCallback((): number => {
    const now = Date.now();
    let bestTrack = 0;
    let earliestTime = trackAvailableAt.current[0];

    for (let i = 0; i < TRACK_COUNT; i++) {
      if (trackAvailableAt.current[i] <= now) {
        return i;
      }
      if (trackAvailableAt.current[i] < earliestTime) {
        earliestTime = trackAvailableAt.current[i];
        bestTrack = i;
      }
    }
    return bestTrack;
  }, []);

  const removeDanmaku = useCallback((instanceKey: string) => {
    setDanmakus(prev => prev.filter(d => d.instanceKey !== instanceKey));
    removeTimers.current.delete(instanceKey);
  }, []);

  const addDanmaku = useCallback((msg: DanmakuMessage) => {
    instanceCounter.current += 1;
    const instanceKey = `${msg.id}-${instanceCounter.current}`;
    const createdAt = Date.now();

    const track = findAvailableTrack();
    const duration = MIN_DURATION + Math.random() * (MAX_DURATION - MIN_DURATION);
    const containerHeight = containerRef.current?.clientHeight || window.innerHeight;
    const trackHeight = containerHeight / TRACK_COUNT;
    const top = track * trackHeight + Math.random() * (trackHeight * 0.3);

    trackAvailableAt.current[track] = createdAt + duration * 1000 * 0.35;

    const item: DanmakuItem = {
      ...msg,
      top,
      duration,
      track,
      instanceKey,
      createdAt,
    };

    setDanmakus(prev => {
      let next = [...prev, item];
      if (next.length > MAX_DANMAKU) {
        const toRemove = next.slice(0, next.length - MAX_DANMAKU);
        toRemove.forEach(d => {
          const timer = removeTimers.current.get(d.instanceKey);
          if (timer) {
            window.clearTimeout(timer);
            removeTimers.current.delete(d.instanceKey);
          }
        });
        next = next.slice(next.length - MAX_DANMAKU);
      }
      return next;
    });

    const removeTimer = window.setTimeout(() => {
      removeDanmaku(instanceKey);
    }, duration * 1000 + 500);

    removeTimers.current.set(instanceKey, removeTimer);
  }, [findAvailableTrack, removeDanmaku]);

  useEffect(() => {
    const unsub1 = eventBus.on('NEW_MESSAGE', (msg: DanmakuMessage) => {
      addDanmaku(msg);
    });

    const unsub2 = eventBus.on('CLEAR_SCREEN', () => {
      setDanmakus([]);
      trackAvailableAt.current = new Array(TRACK_COUNT).fill(0);
      removeTimers.current.forEach(timer => window.clearTimeout(timer));
      removeTimers.current.clear();
    });

    const unsub3 = eventBus.on('SETTING_UPDATED', (data: any) => {
      setSendingEnabled(data?.sendingEnabled ?? true);
    });

    const unsub4 = eventBus.on('WS_CONNECTED', () => {
      eventBus.emit('GET_HISTORY');
    });

    const unsub5 = eventBus.on('HISTORY_MESSAGES', (messages: DanmakuMessage[]) => {
      if (messages && messages.length > 0) {
        messages.forEach((msg, i) => {
          setTimeout(() => addDanmaku(msg), i * 200);
        });
      }
    });

    return () => {
      unsub1();
      unsub2();
      unsub3();
      unsub4();
      unsub5();
      removeTimers.current.forEach(timer => window.clearTimeout(timer));
      removeTimers.current.clear();
    };
  }, [addDanmaku]);

  return (
    <div className="wall-screen" ref={containerRef}>
      <div className="wall-header">
        <div className="wall-logo">🎬 LIVE DANMAKU</div>
        <div className="wall-stats">
          <span>弹幕数: {danmakus.length}/{MAX_DANMAKU}</span>
          <span className={sendingEnabled ? 'status-on' : 'status-off'}>
            {sendingEnabled ? '● 发送开启' : '○ 发送关闭'}
          </span>
        </div>
      </div>

      <div className="danmaku-container">
        {danmakus.map(danmaku => (
          <div
            key={danmaku.instanceKey}
            className={`danmaku-item ${danmaku.sensitive ? 'sensitive' : ''}`}
            style={{
              top: danmaku.top,
              color: danmaku.color,
              animationDuration: `${danmaku.duration}s`,
            }}
          >
            <span className="danmaku-nickname">{danmaku.nickname}:</span>
            <span className="danmaku-content">{danmaku.content}</span>
          </div>
        ))}
      </div>

      {danmakus.length === 0 && (
        <div className="empty-wall">
          <p>暂无弹幕</p>
          <p className="sub">等待第一条精彩消息~</p>
        </div>
      )}
    </div>
  );
};
