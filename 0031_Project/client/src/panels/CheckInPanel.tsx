import { useEffect, useState } from 'react';
import { wsClient } from '../base/wsClient';
import { RoomStatusText, type Room } from '../base/types';
import './CheckInPanel.css';

interface CheckInPanelProps {
  selectedRoom: Room | null;
}

function formatDateTimeLocal(timestamp: number): string {
  const date = new Date(timestamp);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatDateTime(timestamp: number): string {
  const date = new Date(timestamp);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function CheckInPanel({ selectedRoom }: CheckInPanelProps) {
  const [guestName, setGuestName] = useState('');
  const [expectedCheckOut, setExpectedCheckOut] = useState(() => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(12, 0, 0, 0);
    return formatDateTimeLocal(tomorrow.getTime());
  });

  useEffect(() => {
    setGuestName('');
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(12, 0, 0, 0);
    setExpectedCheckOut(formatDateTimeLocal(tomorrow.getTime()));
  }, [selectedRoom?.id]);

  const handleCheckIn = () => {
    if (!selectedRoom) return;
    if (!guestName.trim()) {
      alert('请填写客人姓名');
      return;
    }
    if (!expectedCheckOut) {
      alert('请填写预计离店时间');
      return;
    }
    const expectedTime = new Date(expectedCheckOut).getTime();
    if (expectedTime <= Date.now()) {
      alert('预计离店时间必须晚于当前时间');
      return;
    }
    wsClient.send('CHECK_IN', {
      roomId: selectedRoom.id,
      guestName: guestName.trim(),
      expectedCheckOutTime: expectedTime,
    });
    setGuestName('');
  };

  const handleCheckOut = () => {
    if (!selectedRoom) return;
    if (!confirm(`确认办理退房？`)) return;
    wsClient.send('CHECK_OUT', { roomId: selectedRoom.id });
  };

  const handleClean = () => {
    if (!selectedRoom) return;
    wsClient.send('CLEAN_ROOM', { roomId: selectedRoom.id });
  };

  const handleMaintenance = () => {
    if (!selectedRoom) return;
    wsClient.send('MARK_MAINTENANCE', { roomId: selectedRoom.id });
  };

  const handleRepairDone = () => {
    if (!selectedRoom) return;
    wsClient.send('REPAIR_DONE', { roomId: selectedRoom.id });
  };

  if (!selectedRoom) {
    return (
      <div className="checkin-panel">
        <div className="panel-empty">请在左侧选择一个房间</div>
      </div>
    );
  }

  return (
    <div className="checkin-panel">
      <div className="panel-header">
        <div className="room-title">
          <span className="room-no-label">{selectedRoom.roomNo}</span>
        </div>
        <span className={`status-tag status-${selectedRoom.status.toLowerCase()}`}>
          {RoomStatusText[selectedRoom.status]}
        </span>
      </div>

      <div className="room-info">
        <div className="info-row">
          <span className="info-label">房间号</span>
          <span className="info-value">{selectedRoom.roomNo}</span>
        </div>
        <div className="info-row">
          <span className="info-label">楼层</span>
          <span className="info-value">{selectedRoom.floor} 楼</span>
        </div>
        <div className="info-row">
          <span className="info-label">房型</span>
          <span className="info-value">{selectedRoom.type}</span>
        </div>
        {selectedRoom.currentStay && (
          <>
            <div className="info-row">
              <span className="info-label">入住客人</span>
              <span className="info-value">{selectedRoom.currentStay.guestName}</span>
            </div>
            <div className="info-row">
              <span className="info-label">入住时间</span>
              <span className="info-value">{formatDateTime(selectedRoom.currentStay.checkInTime)}</span>
            </div>
            <div className="info-row">
              <span className="info-label">预计离店</span>
              <span className={`info-value ${selectedRoom.isOverdue ? 'overdue' : ''}`}>
                {formatDateTime(selectedRoom.currentStay.expectedCheckOutTime)}
                {selectedRoom.isOverdue && ' (已超时)'}
              </span>
            </div>
          </>
        )}
      </div>

      {selectedRoom.status === 'VACANT' && (
        <div className="action-section">
          <div className="section-title">办理入住</div>
          <div className="form-group">
            <label>客人姓名</label>
            <input
              type="text"
              placeholder="请输入客人姓名"
              value={guestName}
              onChange={(e) => setGuestName(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>预计离店时间</label>
            <input
              type="datetime-local"
              value={expectedCheckOut}
              onChange={(e) => setExpectedCheckOut(e.target.value)}
            />
          </div>
          <button className="btn btn-primary btn-block" onClick={handleCheckIn}>
            办理入住
          </button>
        </div>
      )}

      {selectedRoom.status === 'OCCUPIED' && (
        <div className="action-section">
          <button className="btn btn-warning btn-block" onClick={handleCheckOut}>
            办理退房
          </button>
          <button className="btn btn-danger btn-block" onClick={handleMaintenance}>
            报修
          </button>
        </div>
      )}

      {selectedRoom.status === 'DIRTY' && (
        <div className="action-section">
          <button className="btn btn-success btn-block" onClick={handleClean}>
          打扫完成
        </button>
          <button className="btn btn-danger btn-block" onClick={handleMaintenance}>
            报修
          </button>
        </div>
      )}

      {selectedRoom.status === 'MAINTENANCE' && (
        <div className="action-section">
          <button className="btn btn-primary btn-block" onClick={handleRepairDone}>
            解除维修
          </button>
        </div>
      )}
    </div>
  );
}
