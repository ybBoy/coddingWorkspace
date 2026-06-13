import React, { useEffect, useState } from 'react';
import { Pet, CareRecord, StatusChange, CARE_ACTIONS, STATUS_LABELS, PetStatus } from '../types';
import { petSocket } from '../core/socket';
import { eventBus } from '../core/EventBus';

interface PetDetailPanelProps {
  petId: string | null;
  onClose: () => void;
  isAdmin: boolean;
  staffName: string;
}

const PetDetailPanel: React.FC<PetDetailPanelProps> = ({ petId, onClose, isAdmin, staffName }) => {
  const [pet, setPet] = useState<Pet | null>(null);
  const [careRecords, setCareRecords] = useState<CareRecord[]>([]);
  const [statusChanges, setStatusChanges] = useState<StatusChange[]>([]);
  const [editing, setEditing] = useState(false);
  const [editName, setEditName] = useState('');
  const [editBreed, setEditBreed] = useState('');
  const [editPhone, setEditPhone] = useState('');

  useEffect(() => {
    if (!petId) return;
    petSocket.getPetDetail(petId);

    const handleDetail = (data: { pet: Pet; careRecords: CareRecord[]; statusChanges: StatusChange[] }) => {
      if (data.pet.id === petId) {
        setPet(data.pet);
        setCareRecords(data.careRecords);
        setStatusChanges(data.statusChanges);
        setEditName(data.pet.name);
        setEditBreed(data.pet.breed);
        setEditPhone(data.pet.ownerPhoneLast4);
      }
    };
    eventBus.on('petDetail', handleDetail);
    return () => { eventBus.off('petDetail', handleDetail); };
  }, [petId]);

  if (!petId || !pet) return null;

  const formatTime = (timeStr: string) => {
    const d = new Date(timeStr);
    return `${d.getMonth()+1}/${d.getDate()} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`;
  };

  const getActionEmoji = (action: string) => {
    const found = CARE_ACTIONS.find((a) => a.value === action);
    return found ? found.emoji : '📝';
  };

  const getActionLabel = (action: string) => {
    const found = CARE_ACTIONS.find((a) => a.value === action);
    return found ? found.label : action;
  };

  const handleSaveEdit = () => {
    if (editName.trim() && editBreed.trim() && editPhone.trim()) {
      petSocket.updatePet(pet.id, editName.trim(), editBreed.trim(), editPhone.trim());
      setEditing(false);
    }
  };

  const handleDeleteRecord = (recordId: string) => {
    if (confirm('确定删除这条护理记录？')) {
      petSocket.deleteCareRecord(recordId);
      setCareRecords((prev) => prev.filter((r) => r.id !== recordId));
    }
  };

  const handleStatusChange = (status: PetStatus) => {
    petSocket.updateStatus(pet.id, status, staffName);
  };

  const timeline = [...careRecords.map((r) => ({ type: 'care' as const, data: r, time: new Date(r.time).getTime() })),
    ...statusChanges.map((c) => ({ type: 'status' as const, data: c, time: new Date(c.time).getTime() }))]
    .sort((a, b) => b.time - a.time);

  return (
    <div className="detail-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="detail-panel">
        <div className="detail-header">
          <h2>{pet.name}</h2>
          <button className="btn-icon" onClick={onClose}>✕</button>
        </div>

        <div className="detail-info">
          {editing ? (
            <div className="detail-edit-form">
              <div className="form-field">
                <label className="form-label">名字</label>
                <input className="form-input" value={editName} onChange={(e) => setEditName(e.target.value)} />
              </div>
              <div className="form-field">
                <label className="form-label">品种</label>
                <input className="form-input" value={editBreed} onChange={(e) => setEditBreed(e.target.value)} />
              </div>
              <div className="form-field">
                <label className="form-label">电话后四位</label>
                <input className="form-input" value={editPhone} onChange={(e) => setEditPhone(e.target.value.replace(/\D/g,'').slice(0,4))} maxLength={4} />
              </div>
              <div className="form-actions">
                <button className="btn-primary" onClick={handleSaveEdit}>保存</button>
                <button className="btn-secondary" onClick={() => setEditing(false)}>取消</button>
              </div>
            </div>
          ) : (
            <div className="detail-info-grid">
              <div className="detail-info-item"><span className="info-label">品种</span><span className="info-value">{pet.breed}</span></div>
              <div className="detail-info-item"><span className="info-label">主人电话</span><span className="info-value">尾号 {pet.ownerPhoneLast4}</span></div>
              <div className="detail-info-item"><span className="info-label">入住时间</span><span className="info-value">{formatTime(pet.checkInTime)}</span></div>
              <div className="detail-info-item"><span className="info-label">当前状态</span><span className="info-value">{STATUS_LABELS[pet.status]}</span></div>
              {isAdmin && <button className="btn-secondary btn-sm" onClick={() => setEditing(true)}>✏️ 修改资料</button>}
            </div>
          )}
        </div>

        <div className="detail-status-actions">
          <span className="detail-section-label">切换状态</span>
          <div className="status-actions">
            <button className={`status-btn status-normal-btn ${pet.status==='NORMAL'?'active':''}`} onClick={() => handleStatusChange('NORMAL')} disabled={pet.status==='NORMAL'}>正常</button>
            <button className={`status-btn status-attention-btn ${pet.status==='NEED_ATTENTION'?'active':''}`} onClick={() => handleStatusChange('NEED_ATTENTION')} disabled={pet.status==='NEED_ATTENTION'}>需要关注</button>
            <button className={`status-btn status-picked-up-btn ${pet.status==='PICKED_UP'?'active':''}`} onClick={() => handleStatusChange('PICKED_UP')} disabled={pet.status==='PICKED_UP'}>已接走</button>
          </div>
        </div>

        <div className="detail-timeline">
          <h3>护理时间线</h3>
          {timeline.length === 0 ? (
            <div className="empty-state"><div className="empty-icon">🐾</div><div className="empty-text">暂无记录</div></div>
          ) : (
            <div className="timeline-list">
              {timeline.map((item) => {
                if (item.type === 'care') {
                  const r = item.data as CareRecord;
                  return (
                    <div key={r.id} className="timeline-item care-item">
                      <div className="timeline-icon">{getActionEmoji(r.action)}</div>
                      <div className="timeline-content">
                        <div className="timeline-main">
                          <span className="timeline-action">{getActionLabel(r.action)}</span>
                          {r.note && <span className="timeline-note"> · {r.note}</span>}
                        </div>
                        <div className="timeline-meta">
                          <span className="timeline-time">{formatTime(r.time)}</span>
                          {r.staffName && <span className="timeline-staff"> · {r.staffName}</span>}
                          {isAdmin && <button className="btn-delete-record" onClick={() => handleDeleteRecord(r.id)}>删除</button>}
                        </div>
                      </div>
                    </div>
                  );
                } else {
                  const c = item.data as StatusChange;
                  return (
                    <div key={c.id} className="timeline-item status-item">
                      <div className="timeline-icon">🔄</div>
                      <div className="timeline-content">
                        <div className="timeline-main">
                          状态变更：{STATUS_LABELS[c.oldStatus]} → {STATUS_LABELS[c.newStatus]}
                        </div>
                        <div className="timeline-meta">
                          <span className="timeline-time">{formatTime(c.time)}</span>
                          {c.staffName && <span className="timeline-staff"> · {c.staffName}</span>}
                        </div>
                      </div>
                    </div>
                  );
                }
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default PetDetailPanel;
