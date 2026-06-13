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
  const [price, setPrice] = useState(198);
  const [deposit, setDeposit] = useState(100);
  const [settleOnCheckOut, setSettleOnCheckOut] = useState(false);
  const [maintenanceRemark, setMaintenanceRemark] = useState('');
  const [showMaintenanceInput, setShowMaintenanceInput] = useState(false);

  useEffect(() => {
    setGuestName('');
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(12, 0, 0, 0);
    setExpectedCheckOut(formatDateTimeLocal(tomorrow.getTime()));
    if (selectedRoom) {
      setPrice(selectedRoom.defaultPrice || 198);
      setDeposit(Math.round((selectedRoom.defaultPrice || 198) * 0.5));
    }
    setShowMaintenanceInput(false);
    setMaintenanceRemark('');
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
      price,
      deposit,
    });
    setGuestName('');
  };

  const handleCheckOut = (settle: boolean) => {
    if (!selectedRoom) return;
    if (!confirm(`确认办理退房？${settle ? '（已结清）' : '（未结清）'}`)) return;
    wsClient.send('CHECK_OUT', { roomId: selectedRoom.id, settle });
  };

  const handleClean = () => {
    if (!selectedRoom) return;
    wsClient.send('CLEAN_ROOM', { roomId: selectedRoom.id });
  };

  const handleMaintenance = () => {
    if (!selectedRoom) return;
    wsClient.send('MARK_MAINTENANCE', {
      roomId: selectedRoom.id,
      remark: maintenanceRemark || undefined,
    });
    setShowMaintenanceInput(false);
    setMaintenanceRemark('');
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

  const stay = selectedRoom.currentStay;

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
          <span className="info-label">房型</span>
          <span className="info-value">{selectedRoom.type}</span>
        </div>
        <div className="info-row">
          <span className="info-label">默认房价</span>
          <span className="info-value price-value">¥{selectedRoom.defaultPrice.toFixed(2)}</span>
        </div>
        {stay && (
          <>
            <div className="info-row">
              <span className="info-label">入住客人</span>
              <span className="info-value">{stay.guestName}</span>
            </div>
            <div className="info-row">
              <span className="info-label">入住时间</span>
              <span className="info-value">{formatDateTime(stay.checkInTime)}</span>
            </div>
            <div className="info-row">
              <span className="info-label">预计离店</span>
              <span className={`info-value ${selectedRoom.isOverdue ? 'overdue' : ''}`}>
                {formatDateTime(stay.expectedCheckOutTime)}
                {selectedRoom.isOverdue && ' ⚠️ 延时未退房'}
              </span>
            </div>
            <div className="info-row">
              <span className="info-label">房价</span>
              <span className="info-value">¥{stay.price.toFixed(2)}</span>
            </div>
            <div className="info-row">
              <span className="info-label">押金</span>
              <span className="info-value">¥{stay.deposit.toFixed(2)}</span>
            </div>
            <div className="info-row">
              <span className="info-label">收款状态</span>
              <span className={`info-value ${stay.settled ? 'settled' : 'unsettled'}`}>
                {stay.settled ? '✅ 已结清' : '❌ 未结清'}
              </span>
            </div>
            <div className="info-row">
              <span className="info-label">入住操作</span>
              <span className="info-value">{stay.checkInOperator}</span>
            </div>
          </>
        )}
      </div>

      {selectedRoom.status === 'DISABLED' && (
        <div className="action-section">
          <div className="disabled-notice">
            <div className="disabled-icon">🚫</div>
            <div className="disabled-text">
              <div className="disabled-title">此房间已停用</div>
              <div className="disabled-hint">请在管理页面中启用后再进行操作</div>
            </div>
          </div>
        </div>
      )}

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
          <div className="form-row">
            <div className="form-group half">
              <label>房价（元）</label>
              <input
                type="number"
                value={price}
                onChange={(e) => setPrice(parseFloat(e.target.value) || 0)}
              />
            </div>
            <div className="form-group half">
              <label>押金（元）</label>
              <input
                type="number"
                value={deposit}
                onChange={(e) => setDeposit(parseFloat(e.target.value) || 0)}
              />
            </div>
          </div>
          <button className="btn btn-primary btn-block" onClick={handleCheckIn}>
            办理入住
          </button>
        </div>
      )}

      {selectedRoom.status === 'OCCUPIED' && (
        <div className="action-section">
          <div className="section-title">退房操作</div>
          <button className="btn btn-success btn-block" onClick={() => handleCheckOut(true)}>
            退房（已结清）
          </button>
          <button className="btn btn-warning btn-block" onClick={() => handleCheckOut(false)}>
            退房（未结清）
          </button>
          <div className="action-divider" />
          {!showMaintenanceInput ? (
            <button className="btn btn-danger btn-block" onClick={() => setShowMaintenanceInput(true)}>
              🔧 报修
            </button>
          ) : (
            <div className="maintenance-form">
              <div className="form-group">
                <label>维修原因</label>
                <input
                  type="text"
                  placeholder="请输入维修原因（选填）"
                  value={maintenanceRemark}
                  onChange={(e) => setMaintenanceRemark(e.target.value)}
                />
              </div>
              <div className="form-actions-row">
                <button className="btn btn-danger" onClick={handleMaintenance}>
                  确认报修
                </button>
                <button className="btn btn-secondary" onClick={() => setShowMaintenanceInput(false)}>
                  取消
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {selectedRoom.status === 'DIRTY' && (
        <div className="action-section">
          <button className="btn btn-success btn-block" onClick={handleClean}>
            🧹 打扫完成
          </button>
          {!showMaintenanceInput ? (
            <button className="btn btn-danger btn-block" onClick={() => setShowMaintenanceInput(true)}>
              🔧 报修
            </button>
          ) : (
            <div className="maintenance-form">
              <div className="form-group">
                <label>维修原因</label>
                <input
                  type="text"
                  placeholder="请输入维修原因（选填）"
                  value={maintenanceRemark}
                  onChange={(e) => setMaintenanceRemark(e.target.value)}
                />
              </div>
              <div className="form-actions-row">
                <button className="btn btn-danger" onClick={handleMaintenance}>
                  确认报修
                </button>
                <button className="btn btn-secondary" onClick={() => setShowMaintenanceInput(false)}>
                  取消
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {selectedRoom.status === 'MAINTENANCE' && (
        <div className="action-section">
          <button className="btn btn-primary btn-block" onClick={handleRepairDone}>
            ✅ 解除维修
          </button>
        </div>
      )}
    </div>
  );
}
