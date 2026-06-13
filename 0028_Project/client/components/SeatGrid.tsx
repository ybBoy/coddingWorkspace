import React from 'react';
import { SeatData } from '../core/EventBus';

interface SeatGridProps {
  seats: SeatData[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  mySeatId: number | null;
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

const SeatGrid: React.FC<SeatGridProps> = ({ seats, selectedId, onSelect, mySeatId }) => {
  const rows: SeatData[][] = [];
  for (let r = 0; r < 8; r++) {
    rows.push(seats.filter((s) => s.row === r).sort((a, b) => a.col - b.col));
  }

  return (
    <div className="seat-grid-wrapper">
      <div className="seat-grid-legend">
        {Object.entries(STATUS_LABEL).map(([key, label]) => (
          <span key={key} className="legend-item">
            <span className="legend-dot" style={{ backgroundColor: STATUS_COLOR[key] }} />
            {label}
          </span>
        ))}
      </div>
      <div className="seat-grid">
        {rows.map((row, ri) => (
          <div key={ri} className="seat-row">
            <span className="row-label">{ri + 1}排</span>
            {row.map((seat) => {
              const isSelected = seat.id === selectedId;
              const isMine = seat.id === mySeatId;
              return (
                <button
                  key={seat.id}
                  className={`seat-cell ${seat.status} ${isSelected ? 'selected' : ''} ${isMine ? 'mine' : ''}`}
                  style={{
                    backgroundColor: STATUS_COLOR[seat.status],
                    opacity: isSelected ? 1 : 0.85,
                    boxShadow: isSelected
                      ? `0 0 0 3px #fff, 0 0 0 5px ${STATUS_COLOR[seat.status]}`
                      : isMine
                      ? `0 0 0 2px #8b5cf6`
                      : undefined,
                  }}
                  onClick={() => onSelect(seat.id)}
                  title={`${ri + 1}排${seat.col + 1}座 - ${STATUS_LABEL[seat.status]}${seat.nickname ? ` - ${seat.nickname}` : ''}`}
                >
                  <span className="seat-id">{seat.col + 1}</span>
                  {seat.nickname && (
                    <span className="seat-nickname">{seat.nickname}</span>
                  )}
                </button>
              );
            })}
          </div>
        ))}
      </div>
      <div className="seat-stats">
        空闲: {seats.filter((s) => s.status === 'free').length} | 占用: {seats.filter((s) => s.status === 'occupied').length} | 暂离: {seats.filter((s) => s.status === 'away').length} | 可释放: {seats.filter((s) => s.status === 'releasable').length}
      </div>
    </div>
  );
};

export default SeatGrid;
