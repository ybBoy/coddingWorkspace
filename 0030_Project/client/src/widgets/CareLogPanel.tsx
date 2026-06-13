import React from 'react';
import { CareRecord, CARE_ACTIONS } from '../types';

interface CareLogPanelProps {
  records: CareRecord[];
}

const CareLogPanel: React.FC<CareLogPanelProps> = ({ records }) => {
  const getActionLabel = (action: string) => {
    const found = CARE_ACTIONS.find((a) => a.value === action);
    return found ? found.label : action;
  };

  const getActionEmoji = (action: string) => {
    const found = CARE_ACTIONS.find((a) => a.value === action);
    return found ? found.emoji : '📝';
  };

  const formatTime = (timeStr: string) => {
    const date = new Date(timeStr);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  };

  return (
    <div className="care-log-panel">
      <div className="panel-header">
        <h3>最近护理动态</h3>
        <span className="record-count">共 {records.length} 条</span>
      </div>

      <div className="care-log-list">
        {records.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">🐾</div>
            <div className="empty-text">暂无护理记录</div>
          </div>
        ) : (
          records.map((record) => (
            <div key={record.id} className="care-log-item">
              <div className="log-time">{formatTime(record.time)}</div>
              <div className="log-emoji">{getActionEmoji(record.action)}</div>
              <div className="log-content">
                <div className="log-pet-name">{record.petName}</div>
                <div className="log-action">{getActionLabel(record.action)}</div>
                {record.note && <div className="log-note">{record.note}</div>}
                {record.staffName && <div className="log-staff">👤 {record.staffName}</div>}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default CareLogPanel;
