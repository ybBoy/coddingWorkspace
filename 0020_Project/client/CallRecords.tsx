/**
 * CallRecords 叫号记录组件
 * 职责：显示底部最近 10 条叫号记录（叫号/完成/过号/重叫）
 * 订阅 EventBus 的 QUEUE_STATE_UPDATED 事件获取最新状态
 */
import React, { useEffect, useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { QueueState, CallRecord } from './types';

const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

const CallRecords: React.FC = () => {
  const [state, setState] = useState<QueueState | null>(null);

  useEffect(() => {
    const unsubscribe = eventBus.on(EVENTS.QUEUE_STATE_UPDATED, (newState: QueueState) => {
      setState(newState);
    });
    return unsubscribe;
  }, []);

  if (!state) return null;

  const { callRecords } = state;

  return (
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
  );
};

export default CallRecords;
