import React from 'react';
import { SeatData } from '../core/EventBus';
import { sendAction } from '../core/socket';

interface SeatInfoPanelProps {
  seat: SeatData | null;
  nickname: string;
  mySeatId: number | null;
}

const STATUS_TEXT: Record<string, string> = {
  free: '空闲',
  occupied: '已占用',
  away: '暂离中',
  releasable: '可释放',
};

const SeatInfoPanel: React.FC<SeatInfoPanelProps> = ({ seat, nickname, mySeatId }) => {
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
  const awayMinutes = seat.awaySince > 0
    ? Math.floor((Date.now() - seat.awaySince) / 60000)
    : 0;

  const handleSit = () => {
    sendAction('sit', { seatId: seat.id, nickname });
  };

  const handleAway = () => {
    sendAction('away', { seatId: seat.id, nickname });
  };

  const handleLeave = () => {
    sendAction('leave', { seatId: seat.id, nickname });
  };

  const handleForceRelease = () => {
    if (window.confirm(`确认强制释放 ${seat.row + 1}排${seat.col + 1}座？`)) {
      sendAction('forceRelease', { seatId: seat.id });
    }
  };

  return (
    <div className="seat-info-panel">
      <h3>座位详情</h3>
      <div className="info-row">
        <span className="info-label">位置</span>
        <span className="info-value">{seat.row + 1}排 {seat.col + 1}座</span>
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
      {seat.status === 'away' && (
        <div className="info-row">
          <span className="info-label">暂离时长</span>
          <span className="info-value">{awayMinutes} 分钟</span>
        </div>
      )}
      {isReleasable && (
        <div className="info-row warning">
          <span>⚠ 该座位暂离超过15分钟，管理员可强制释放</span>
        </div>
      )}

      <div className="info-actions">
        {isFree && nickname && (
          <button className="btn btn-sit" onClick={handleSit} disabled={!nickname.trim()}>
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
        {(isReleasable || (seat.status !== 'free' && !isMine)) && (
          <button className="btn btn-force" onClick={handleForceRelease}>
            强制释放
          </button>
        )}
      </div>
    </div>
  );
};

export default SeatInfoPanel;
