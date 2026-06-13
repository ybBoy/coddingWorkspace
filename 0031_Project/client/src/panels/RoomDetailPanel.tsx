import { useEffect, useState } from 'react';
import { eventBus, Events } from '../base/EventBus';
import { wsClient } from '../base/wsClient';
import type { RoomDetail, StayRecord, RoomLog } from '../base/types';

interface Props {
  roomId: string | null;
}

type DetailTab = 'stay' | 'maintenance' | 'cleaning';

export function RoomDetailPanel({ roomId }: Props) {
  const [detail, setDetail] = useState<RoomDetail | null>(null);
  const [activeTab, setActiveTab] = useState<DetailTab>('stay');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!roomId) {
      setDetail(null);
      return;
    }

    const detailHandler = (data: RoomDetail) => {
      if (data.room && data.room.id === roomId) {
        setDetail(data);
        setLoading(false);
      }
    };

    eventBus.on(Events.ROOM_DETAIL, detailHandler);

    setLoading(true);
    setDetail(null);
    wsClient.requestRoomDetail(roomId);

    return () => eventBus.off(Events.ROOM_DETAIL, detailHandler);
  }, [roomId]);

  if (!roomId) {
    return (
      <div className="room-detail-panel">
        <div className="empty-state">请在左侧选择一个房间查看详情</div>
      </div>
    );
  }

  if (loading && !detail) {
    return (
      <div className="room-detail-panel">
        <div className="loading">加载中...</div>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="room-detail-panel">
        <div className="empty-state">未获取到房间信息</div>
      </div>
    );
  }

  const { room, stayHistory, logs } = detail;

  const maintenanceLogs = logs.filter((l) => l.action === '报修' || l.action === '解除维修');
  const cleaningLogs = logs.filter((l) => l.action === '打扫完成' || l.action === '批量打扫' || l.action === '退房');

  const formatDateTime = (ts: number) => {
    const d = new Date(ts);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  };

  const calculateNights = (checkIn: number, checkOut: number | null | undefined) => {
    if (!checkOut) return 0;
    const ms = checkOut - checkIn;
    return Math.ceil(ms / (1000 * 60 * 60 * 24));
  };

  return (
    <div className="room-detail-panel">
      <h3 className="panel-title">房间详情 - {room.roomNo}</h3>
      <div className="room-basic-info">
        <div className="info-row">
          <span className="info-label">房型</span>
          <span className="info-value">{room.type}</span>
        </div>
        <div className="info-row">
          <span className="info-label">默认房价</span>
          <span className="info-value price">¥{room.defaultPrice.toFixed(2)}</span>
        </div>
      </div>

      <div className="detail-tabs">
        <button
          className={`tab-btn ${activeTab === 'stay' ? 'active' : ''}`}
          onClick={() => setActiveTab('stay')}
        >
          🛏️ 入住记录 ({stayHistory.length})
        </button>
        <button
          className={`tab-btn ${activeTab === 'maintenance' ? 'active' : ''}`}
          onClick={() => setActiveTab('maintenance')}
        >
          🔧 维修记录 ({maintenanceLogs.length})
        </button>
        <button
          className={`tab-btn ${activeTab === 'cleaning' ? 'active' : ''}`}
          onClick={() => setActiveTab('cleaning')}
        >
          🧹 打扫记录 ({cleaningLogs.length})
        </button>
      </div>

      <div className="tab-content">
        {activeTab === 'stay' && (
          <div className="stay-history-list">
            {stayHistory.length === 0 ? (
              <div className="empty-list">暂无历史入住记录</div>
            ) : (
              stayHistory.map((record) => <StayRecordItem key={record.id} record={record} formatDateTime={formatDateTime} calculateNights={calculateNights} />)
            )}
          </div>
        )}

        {activeTab === 'maintenance' && (
          <div className="log-list">
            {maintenanceLogs.length === 0 ? (
              <div className="empty-list">暂无维修记录</div>
            ) : (
              maintenanceLogs.map((log) => <LogItem key={log.id} log={log} formatDateTime={formatDateTime} />)
            )}
          </div>
        )}

        {activeTab === 'cleaning' && (
          <div className="log-list">
            {cleaningLogs.length === 0 ? (
              <div className="empty-list">暂无打扫记录</div>
            ) : (
              cleaningLogs.map((log) => <LogItem key={log.id} log={log} formatDateTime={formatDateTime} />)
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function StayRecordItem({
  record,
  formatDateTime,
  calculateNights,
}: {
  record: StayRecord;
  formatDateTime: (ts: number) => string;
  calculateNights: (inTs: number, outTs: number | null | undefined) => number;
}) {
  const nights = calculateNights(record.checkInTime, record.actualCheckOutTime);
  const totalPrice = nights * record.price;
  const settledClass = record.settled ? 'status-settled' : 'status-unsettled';

  return (
    <div className="stay-record-item">
      <div className="stay-header">
        <span className="guest-name">👤 {record.guestName}</span>
        <span className={`settled-badge ${settledClass}`}>
          {record.settled ? '已结清' : '未结清'}
        </span>
      </div>
      <div className="stay-dates">
        <span>入住：{formatDateTime(record.checkInTime)}</span>
        <span>离店：{record.actualCheckOutTime ? formatDateTime(record.actualCheckOutTime) : '-'}</span>
      </div>
      <div className="stay-financial">
        <span>房价：¥{record.price.toFixed(2)}</span>
        <span>押金：¥{record.deposit.toFixed(2)}</span>
        <span>共 {nights} 晚</span>
        <span className="total-price">合计：¥{totalPrice.toFixed(2)}</span>
      </div>
      <div className="stay-operators">
        <span>入住操作：{record.checkInOperator}</span>
        {record.checkOutOperator && <span>退房操作：{record.checkOutOperator}</span>}
      </div>
    </div>
  );
}

function LogItem({
  log,
  formatDateTime,
}: {
  log: RoomLog;
  formatDateTime: (ts: number) => string;
}) {
  const actionClass =
    log.action === '报修'
      ? 'action-maintenance'
      : log.action === '解除维修'
        ? 'action-repair-done'
        : log.action === '打扫完成' || log.action === '批量打扫'
          ? 'action-clean'
          : '';

  return (
    <div className={`log-item ${actionClass}`}>
      <div className="log-time">{formatDateTime(log.timestamp)}</div>
      <div className="log-action">{log.action}</div>
      <div className="log-operator">操作人：{log.operator}</div>
      {log.remark && <div className="log-remark">{log.remark}</div>}
    </div>
  );
}
