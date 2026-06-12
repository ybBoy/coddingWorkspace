/**
 * WaitingQueue 等待队列组件
 * 职责：显示左侧等待队列，按取号时间排序
 * 订阅 EventBus 的 QUEUE_STATE_UPDATED 事件获取最新状态
 */
import React, { useEffect, useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { QueueState, Ticket } from './types';

const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

const WaitingQueue: React.FC = () => {
  const [state, setState] = useState<QueueState | null>(null);

  useEffect(() => {
    const unsubscribe = eventBus.on(EVENTS.QUEUE_STATE_UPDATED, (newState: QueueState) => {
      setState(newState);
    });
    return unsubscribe;
  }, []);

  if (!state) return null;

  const { waitingQueue } = state;

  return (
    <section className="waiting-queue">
      <h2 className="section-title">等待队列 ({waitingQueue.length})</h2>
      <div className="queue-list">
        {waitingQueue.length === 0 ? (
          <div className="empty-queue">暂无等待人员</div>
        ) : (
          waitingQueue.map((ticket: Ticket) => (
            <div key={ticket.id} className="queue-item">
              <span className="ticket-number">
                {String(ticket.number).padStart(3, '0')}
              </span>
              <div className="ticket-info">
                <span className="business-tag">{ticket.businessType}</span>
                <span className="queue-time">{formatTime(ticket.createdAt)}</span>
              </div>
            </div>
          ))
        )}
      </div>
    </section>
  );
};

export default WaitingQueue;
