/**
 * ConnectionStatus 连接状态组件
 * 职责：右上角显示 WebSocket 连接状态（在线/重连中/离线）
 * 订阅 EventBus 的 CONNECTION_STATUS_CHANGED 事件
 */
import React, { useEffect, useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { ConnectionStatus as ConnStatus } from './types';

const statusConfig: Record<ConnStatus, { label: string; className: string; dot: string }> = {
  online: { label: '在线', className: 'status-online', dot: 'status-dot-green' },
  reconnecting: { label: '重连中', className: 'status-reconnecting', dot: 'status-dot-orange' },
  offline: { label: '离线', className: 'status-offline', dot: 'status-dot-red' },
};

const ConnectionStatus: React.FC = () => {
  const [status, setStatus] = useState<ConnStatus>('reconnecting');

  useEffect(() => {
    const unsubscribe = eventBus.on(EVENTS.CONNECTION_STATUS_CHANGED, (s: ConnStatus) => {
      setStatus(s);
    });
    return unsubscribe;
  }, []);

  const cfg = statusConfig[status];

  return (
    <div className={`connection-status ${cfg.className}`} title={`连接状态：${cfg.label}`}>
      <span className={`status-dot ${cfg.dot}`} />
      <span className="status-text">{cfg.label}</span>
    </div>
  );
};

export default ConnectionStatus;
