import React, { useEffect, useState } from 'react';
import { ShiftSummary as ShiftSummaryType, STATUS_LABELS } from '../types';
import { petSocket } from '../core/socket';
import { eventBus } from '../core/EventBus';

const ShiftSummary: React.FC = () => {
  const [summary, setSummary] = useState<ShiftSummaryType | null>(null);
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    const handleSummary = (data: ShiftSummaryType) => {
      setSummary(data);
    };
    eventBus.on('shiftSummary', handleSummary);
    return () => { eventBus.off('shiftSummary', handleSummary); };
  }, []);

  const handleOpen = () => {
    const next = !isOpen;
    setIsOpen(next);
    if (next) {
      petSocket.getShiftSummary();
    }
  };

  return (
    <div className="shift-summary">
      <button className="shift-summary-btn" onClick={handleOpen}>
        📋 交接班摘要
        <span className="btn-arrow">{isOpen ? '▲' : '▼'}</span>
      </button>

      {isOpen && summary && (
        <div className="shift-summary-content">
          <div className="summary-stat">
            <span className="stat-value">{summary.totalPetsInStore}</span>
            <span className="stat-label">在店宠物</span>
          </div>
          <div className="summary-stat">
            <span className="stat-value attention">{summary.attentionPets.length}</span>
            <span className="stat-label">待关注</span>
          </div>
          <div className="summary-stat">
            <span className="stat-value picked-up">{summary.pickedUpPets.length}</span>
            <span className="stat-label">已接走</span>
          </div>
          <div className="summary-stat">
            <span className="stat-value">{summary.todayRecordCount}</span>
            <span className="stat-label">今日护理</span>
          </div>

          {summary.attentionPets.length > 0 && (
            <div className="summary-section">
              <div className="summary-section-title">⚠️ 待关注宠物</div>
              {summary.attentionPets.map((pet) => (
                <div key={pet.id} className="summary-pet-item attention">{pet.name} ({pet.breed})</div>
              ))}
            </div>
          )}

          {summary.pickedUpPets.length > 0 && (
            <div className="summary-section">
              <div className="summary-section-title">👋 今日已接走</div>
              {summary.pickedUpPets.map((pet) => (
                <div key={pet.id} className="summary-pet-item picked-up">{pet.name} ({pet.breed})</div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default ShiftSummary;
