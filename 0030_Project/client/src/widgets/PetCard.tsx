import React, { useState } from 'react';
import { Pet, PetStatus, CARE_ACTIONS, STATUS_LABELS } from '../types';
import { petSocket } from '../core/socket';

interface PetCardProps {
  pet: Pet;
  lastCareTime: string | null;
  needsAttention: boolean;
  staffName: string;
  isAdmin: boolean;
  onOpenDetail: (petId: string) => void;
}

const PetCard: React.FC<PetCardProps> = ({ pet, lastCareTime, needsAttention, staffName, isAdmin, onOpenDetail }) => {
  const [showActions, setShowActions] = useState(false);

  const formatTime = (timeStr: string) => {
    const date = new Date(timeStr);
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${month}/${day} ${hours}:${minutes}`;
  };

  const handleStatusChange = (status: PetStatus) => {
    petSocket.updateStatus(pet.id, status, staffName);
    setShowActions(false);
  };

  const handleCareAction = (action: string) => {
    petSocket.addCareRecord(pet.id, action, '', staffName);
    setShowActions(false);
  };

  const getStatusClass = () => {
    if (pet.status === 'PICKED_UP') return 'status-picked-up';
    if (pet.status === 'NEED_ATTENTION' || needsAttention) return 'status-attention';
    return 'status-normal';
  };

  return (
    <div className={`pet-card ${getStatusClass()}`}>
      <div className="pet-card-header" onClick={() => onOpenDetail(pet.id)} style={{ cursor: 'pointer' }}>
        <div className="pet-name">{pet.name}</div>
        <div className="pet-status-badge">{STATUS_LABELS[pet.status]}</div>
      </div>

      <div className="pet-breed">{pet.breed}</div>

      <div className="pet-info-row">
        <span className="info-label">主人电话</span>
        <span className="info-value">尾号 {pet.ownerPhoneLast4}</span>
      </div>

      <div className="pet-info-row">
        <span className="info-label">入住时间</span>
        <span className="info-value">{formatTime(pet.checkInTime)}</span>
      </div>

      {lastCareTime && (
        <div className="pet-info-row">
          <span className="info-label">上次护理</span>
          <span className="info-value">{formatTime(lastCareTime)}</span>
        </div>
      )}

      {needsAttention && pet.status !== 'PICKED_UP' && (
        <div className="attention-warning">
          <span className="warning-icon">⚠️</span>
          <span>超时未护理，请关注</span>
        </div>
      )}

      <div className="pet-card-actions">
        <button className="btn-link" onClick={() => onOpenDetail(pet.id)}>查看详情</button>
        <button className="btn-primary" onClick={() => setShowActions(!showActions)}>
          {showActions ? '收起' : '操作'}
        </button>
      </div>

      {showActions && (
        <div className="action-panel">
          <div className="action-section">
            <div className="action-section-title">护理记录</div>
            <div className="care-actions-grid">
              {CARE_ACTIONS.map((action) => (
                <button
                  key={action.value}
                  className="care-action-btn"
                  onClick={() => handleCareAction(action.value)}
                  disabled={pet.status === 'PICKED_UP'}
                >
                  <span className="care-emoji">{action.emoji}</span>
                  <span className="care-label">{action.label}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="action-section">
            <div className="action-section-title">状态更新</div>
            <div className="status-actions">
              <button
                className="status-btn status-normal-btn"
                onClick={() => handleStatusChange('NORMAL')}
                disabled={pet.status === 'NORMAL'}
              >
                正常
              </button>
              <button
                className="status-btn status-attention-btn"
                onClick={() => handleStatusChange('NEED_ATTENTION')}
                disabled={pet.status === 'NEED_ATTENTION'}
              >
                需要关注
              </button>
              <button
                className="status-btn status-picked-up-btn"
                onClick={() => handleStatusChange('PICKED_UP')}
                disabled={pet.status === 'PICKED_UP'}
              >
                已接走
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PetCard;
