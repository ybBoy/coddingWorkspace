import React, { useState, useEffect } from 'react';
import { SeatData, ZoneType, ZONE_LABELS } from '../core/EventBus';

interface SeatGridProps {
  seats: SeatData[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  mySeatId: number | null;
  activeZone: ZoneType | 'all';
}

const STATUS_COLOR: Record<string, string> = {
  free: '#22c55e',
  occupied: '#3b82f6',
  away: '#f97316',
  releasable: '#ef4444',
};

const STATUS_LABEL: Record<string, string> = {
  free: '空闲',
  occupied: '占用',
  away: '暂离',
  releasable: '可释放',
};

const AWAY_TIMEOUT_MS = 15 * 60 * 1000;

const SeatGrid: React.FC<SeatGridProps> = ({ seats, selectedId, onSelect, mySeatId, activeZone }) => {
  const [scale, setScale] = useState(1);
  const [, setTick] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => setTick((t) => t + 1), 1000);
    return () => clearInterval(timer);
  }, []);

  const rows: SeatData[][] = [];
  for (let r = 0; r < 8; r++) {
    rows.push(seats.filter((s) => s.row === r).sort((a, b) => a.col - b.col));
  }

  const filteredSeats = activeZone === 'all' ? seats : seats.filter((s) => s.zone === activeZone);
  const isFiltered = activeZone !== 'all';

  const handleZoomIn = () => setScale((s) => Math.min(s + 0.1, 1.5));
  const handleZoomOut = () => setScale((s) => Math.max(s - 0.1, 0.5));
  const handleReset = () => setScale(1);

  const getAwayRemaining = (seat: SeatData) => {
    if (seat.status !== 'away' && seat.status !== 'releasable') return null;
    if (seat.awaySince <= 0) return null;
    const elapsed = Date.now() - seat.awaySince;
    const remaining = AWAY_TIMEOUT_MS - elapsed;
    return Math.max(0, Math.floor(remaining / 1000));
  };

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  const filteredStats = {
    free: filteredSeats.filter((s) => s.status === 'free').length,
    occupied: filteredSeats.filter((s) => s.status === 'occupied').length,
    away: filteredSeats.filter((s) => s.status === 'away').length,
    releasable: filteredSeats.filter((s) => s.status === 'releasable').length,
  };

  return (
    <div className="seat-grid-wrapper">
      <div className="seat-grid-toolbar">
        <div className="seat-grid-legend">
          {Object.entries(STATUS_LABEL).map(([key, label]) => (
            <span key={key} className="legend-item">
              <span className="legend-dot" style={{ backgroundColor: STATUS_COLOR[key] }} />
              {label}
            </span>
          ))}
        </div>
        <div className="zoom-controls">
          <button className="zoom-btn" onClick={handleZoomOut} title="缩小">−</button>
          <span className="zoom-level">{Math.round(scale * 100)}%</span>
          <button className="zoom-btn" onClick={handleZoomIn} title="放大">+</button>
          <button className="zoom-btn zoom-reset" onClick={handleReset} title="重置">⟲</button>
        </div>
      </div>

      {isFiltered && (
        <div className="filter-notice">
          当前显示：<strong>{ZONE_LABELS[activeZone as ZoneType]}</strong>（共 {filteredSeats.length} 个座位）
        </div>
      )}

      <div className="seat-grid-container">
        <div className="seat-grid" style={{ transform: `scale(${scale})`, transformOrigin: 'top left' }}>
          {rows.map((row, ri) => (
            <div key={ri} className="seat-row">
              <span className="row-label">{ri + 1}排</span>
              {row.map((seat) => {
                const isSelected = seat.id === selectedId;
                const isMine = seat.id === mySeatId;
                const isDimmed = isFiltered && seat.zone !== activeZone;
                const remaining = getAwayRemaining(seat);
                const isWarning = remaining !== null && remaining < 60 && remaining > 0;

                return (
                  <button
                    key={seat.id}
                    className={`seat-cell ${seat.status} ${isSelected ? 'selected' : ''} ${isMine ? 'mine' : ''} ${isDimmed ? 'dimmed' : ''} zone-${seat.zone}`}
                    style={{
                      backgroundColor: STATUS_COLOR[seat.status],
                      boxShadow: isSelected
                        ? `0 0 0 3px #fff, 0 0 0 5px ${STATUS_COLOR[seat.status]}`
                        : isMine
                        ? `0 0 0 2px #8b5cf6`
                        : undefined,
                    }}
                    onClick={() => onSelect(seat.id)}
                    title={`${ri + 1}排${seat.col + 1}座 - ${STATUS_LABEL[seat.status]}${seat.nickname ? ` - ${seat.nickname}` : ''} - ${ZONE_LABELS[seat.zone] || '普通区'}`}
                  >
                    <span className="seat-id">{seat.col + 1}</span>
                    {seat.nickname && (
                      <span className="seat-nickname">{seat.nickname}</span>
                    )}
                    {remaining !== null && (
                      <span className={`seat-timer ${seat.status === 'releasable' ? 'expired' : ''} ${isWarning ? 'warning' : ''}`}>
                        {seat.status === 'releasable' ? '已超时' : formatTime(remaining)}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          ))}
        </div>
      </div>

      <div className="seat-stats">
        空闲: {filteredStats.free} | 占用: {filteredStats.occupied} | 暂离: {filteredStats.away} | 可释放: {filteredStats.releasable}
      </div>
    </div>
  );
};

export default SeatGrid;
