import React, { useState, useEffect } from 'react';
import { SeatData, eventBus, ZONE_LABELS, ZoneType } from '../core/EventBus';

interface SeatInfoPanelProps {
  seat: SeatData | null;
  nickname: string;
  mySeatId: number | null;
  isAdmin?: boolean;
}

const STATUS_TEXT: Record<string, string> = {
  free: '空闲',
  occupied: '已占用',
  away: '暂离中',
  releasable: '可释放',
};

const AWAY_TIMEOUT_MS = 15 * 60 * 1000;

const SeatInfoPanel: React.FC<SeatInfoPanelProps> = ({ seat, nickname, mySeatId, isAdmin = false }) => {
  const [, setTick] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => setTick((t) => t + 1), 1000);
    return () => clearInterval(timer);
  }, []);

  if (!seat) {
    return (
      <div className="seat-info-panel">
        <h3>座位详情</h3>
        <p className="no-selection">请点击左侧座位查看详情</p>
      </div>
    );
  }

  const isMine = seat.id === mySeatId;
  const isFree = seat.status === 'free';
  const isReleasable = seat.status === 'releasable';

  const getAwayRemaining = () => {
    if (seat.status !== 'away' && seat.status !== 'releasable') return null;
    if (seat.awaySince <= 0) return null;
    const elapsed = Date.now() - seat.awaySince;
    const remaining = AWAY_TIMEOUT_MS - elapsed;
    return Math.max(0, Math.floor(remaining / 1000));
  };

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}分${s.toString().padStart(2, '0')}秒`;
  };

  const remaining = getAwayRemaining();
  const isWarning = remaining !== null && remaining < 60 && remaining > 0;

  const handleSit = () => {
    if (!nickname.trim()) {
      eventBus.emit('toast:show', { message: '请先输入昵称', type: 'warning' });
      return;
    }
    eventBus.emit('seat:sit', { seatId: seat.id, nickname: nickname.trim() });
  };

  const handleAway = () => {
    eventBus.emit('seat:away', { seatId: seat.id, nickname });
  };

  const handleLeave = () => {
    eventBus.emit('seat:leave', { seatId: seat.id, nickname });
  };

  const handleForceRelease = () => {
    eventBus.emit('seat:forceRelease', { seatId: seat.id, isAdmin: true });
    eventBus.emit('toast:show', { message: `已释放 ${seat.row + 1}排${seat.col + 1}座`, type: 'success' });
  };

  return (
    <div className="seat-info-panel">
      <h3>座位详情</h3>
      <div className="info-row">
        <span className="info-label">位置</span>
        <span className="info-value">{seat.row + 1}排 {seat.col + 1}座</span>
      </div>
      <div className="info-row">
        <span className="info-label">区域</span>
        <span className={`info-value zone-badge zone-${seat.zone}`}>
          {ZONE_LABELS[seat.zone as ZoneType] || '普通区'}
        </span>
      </div>
      <div className="info-row">
        <span className="info-label">状态</span>
        <span className={`info-value status-badge ${seat.status}`}>
          {STATUS_TEXT[seat.status]}
        </span>
      </div>
      {seat.nickname && (
        <div className="info-row">
          <span className="info-label">使用者</span>
          <span className="info-value">{seat.nickname}</span>
        </div>
      )}
      {remaining !== null && (
        <div className={`info-row ${isWarning ? 'warning' : ''} ${isReleasable ? 'danger' : ''}`}>
          <span className="info-label">{isReleasable ? '超时情况' : '暂离剩余'}</span>
          <span className="info-value">
            {isReleasable ? '已超过15分钟' : formatTime(remaining)}
            {isWarning && !isReleasable && ' ⚠即将超时'}
          </span>
        </div>
      )}
      {isReleasable && (
        <div className="info-row danger">
          <span>⚠ 该座位暂离超过15分钟，管理员可强制释放</span>
        </div>
      )}
      {isMine && !isFree && (
        <div className="info-row mine-hint">
          <span>👤 这是你当前的座位</span>
        </div>
      )}

      <div className="info-actions">
        {isFree && nickname && (
          <button
            className="btn btn-sit"
            onClick={handleSit}
            disabled={!nickname.trim()}
          >
            入座
          </button>
        )}
        {isMine && seat.status === 'occupied' && (
          <button className="btn btn-away" onClick={handleAway}>
            暂离
          </button>
        )}
        {isMine && (seat.status === 'occupied' || seat.status === 'away') && (
          <button className="btn btn-leave" onClick={handleLeave}>
            离开
          </button>
        )}
        {isAdmin && seat.status !== 'free' && (
          <button className="btn btn-force" onClick={handleForceRelease}>
            强制释放
          </button>
        )}
      </div>
    </div>
  );
};

export default SeatInfoPanel;
