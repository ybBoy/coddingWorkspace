/**
 * QueueBoard 队列展示组件
 * 职责：显示顶部当前叫号、左侧等待队列、底部叫号记录
 * 订阅 EventBus 的 QUEUE_STATE_UPDATED 事件获取最新状态
 */
import React, { useEffect, useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { QueueState, Ticket, CallRecord, Counter } from './types';

const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

const getCounterName = (counters: Counter[], counterId?: string): string => {
  if (!counterId) return '';
  const counter = counters.find((c) => c.id === counterId);
  return counter ? counter.name : '';
};

const QueueBoard: React.FC = () => {
  const [state, setState] = useState<QueueState | null>(null);

  useEffect(() => {
    const unsubscribe = eventBus.on(EVENTS.QUEUE_STATE_UPDATED, (newState: QueueState) => {
      setState(newState);
    });
    return unsubscribe;
  }, []);

  if (!state) {
    return <div className="header"><h1>正在连接服务器...</h1></div>;
  }

  const { waitingQueue, counters, callRecords, currentCalling } = state;

  return (
    <>
      <header className="header">
        <h1>实时排队叫号系统</h1>
        <div className="current-calling">
          <div className="current-ticket">
            <span className="label">当前叫号</span>
            <span className="number">
              {currentCalling ? String(currentCalling.number).padStart(3, '0') : '---'}
            </span>
            <span className="counter">
              {currentCalling
                ? getCounterName(counters, currentCalling.counterId)
                : '暂无叫号'}
            </span>
            {currentCalling && (
              <span className="business-type">{currentCalling.businessType}</span>
            )}
          </div>
          <div className="waiting-count">
            <div className="count">{waitingQueue.length}</div>
            <div className="label">等待人数</div>
          </div>
        </div>
      </header>

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

      <section className="call-records">
        <h2 className="section-title">最近叫号记录</h2>
        <div className="records-grid">
          {callRecords.length === 0 ? (
            <div className="empty-queue">暂无记录</div>
          ) : (
            callRecords.slice(0, 10).map((record: CallRecord, index: number) => (
              <div key={`${record.ticket.id}-${index}`} className="record-item">
                <div>
                  <span className="record-number">
                    {String(record.ticket.number).padStart(3, '0')}
                  </span>
                  <span className={`record-action ${record.action}`}>
                    {record.action === 'called' && '叫号'}
                    {record.action === 'completed' && '完成'}
                    {record.action === 'missed' && '过号'}
                    {record.action === 'recalled' && '重叫'}
                  </span>
                </div>
                <div className="record-counter">
                  {record.counterName} · {formatTime(record.timestamp)}
                </div>
              </div>
            ))
          )}
        </div>
      </section>
    </>
  );
};

export default QueueBoard;
