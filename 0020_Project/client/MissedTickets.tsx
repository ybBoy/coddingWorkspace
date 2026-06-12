/**
 * MissedTickets 过号管理组件
 * 职责：显示过号列表，支持重新加入等待队列、直接重叫、标记结束
 * 订阅 EventBus 获取队列状态，发布过号操作事件
 */
import React, { useEffect, useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { QueueState, Ticket } from './types';

const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

const MissedTickets: React.FC = () => {
  const [missedQueue, setMissedQueue] = useState<Ticket[]>([]);
  const [selectedCounterId, setSelectedCounterId] = useState<string | null>(null);
  const [counters, setCounters] = useState<any[]>([]);

  useEffect(() => {
    const unsub = eventBus.on(EVENTS.QUEUE_STATE_UPDATED, (state: QueueState) => {
      setMissedQueue(state.missedQueue || []);
      setCounters(state.counters.filter((c) => c.enabled));
      if (!selectedCounterId && state.counters.length > 0) {
        const firstEnabled = state.counters.find((c) => c.enabled);
        if (firstEnabled) setSelectedCounterId(firstEnabled.id);
      }
    });
    return unsub;
  }, [selectedCounterId]);

  const handleRequeue = (ticketId: string) => {
    eventBus.emit(EVENTS.REQUEUE_MISSED, { ticketId });
  };

  const handleRecall = (ticketId: string) => {
    if (!selectedCounterId) {
      eventBus.emit(EVENTS.SHOW_TOAST, {
        id: 'warn-' + Date.now(),
        type: 'warning',
        title: '请先选择窗口',
        message: '重叫前需要在右侧操作面板选择一个窗口',
        duration: 3000,
      });
      return;
    }
    eventBus.emit(EVENTS.RECALL_TICKET, { counterId: selectedCounterId, ticketId });
  };

  const handleFinish = (ticketId: string) => {
    eventBus.emit(EVENTS.FINISH_MISSED, { ticketId });
  };

  return (
    <section className="missed-tickets">
      <h2 className="section-title">
        过号列表 ({missedQueue.length})
        {missedQueue.length > 0 && counters.length > 0 && (
          <span className="section-sub">
            重叫窗口：
            <select
              value={selectedCounterId || ''}
              onChange={(e) => setSelectedCounterId(e.target.value)}
              style={{ marginLeft: 8, fontSize: '0.875rem' }}
            >
              {counters.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </span>
        )}
      </h2>
      <div className="queue-list">
        {missedQueue.length === 0 ? (
          <div className="empty-queue">暂无过号</div>
        ) : (
          missedQueue.map((ticket) => (
            <div key={ticket.id} className="queue-item missed-item">
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <span className="ticket-number" style={{ color: 'var(--warning-color)' }}>
                  {String(ticket.number).padStart(3, '0')}
                </span>
                <div className="ticket-info">
                  <span className="business-tag" style={{ background: '#fed7aa', color: 'var(--warning-color)' }}>
                    {ticket.businessType}
                  </span>
                  <span className="queue-time">过号于 {formatTime(ticket.calledAt || ticket.createdAt)}</span>
                </div>
              </div>
              <div className="missed-actions">
                <button className="btn btn-mini btn-primary" onClick={() => handleRequeue(ticket.id)}>
                  重新入队
                </button>
                <button className="btn btn-mini btn-primary-outline" onClick={() => handleRecall(ticket.id)}>
                  直接重叫
                </button>
                <button className="btn btn-mini btn-success" onClick={() => handleFinish(ticket.id)}>
                  结束
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </section>
  );
};

export default MissedTickets;
