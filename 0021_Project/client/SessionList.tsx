import React from 'react';
import { Session } from './types';
import { eventBus, EVENTS } from './EventBus';

// SessionList 场次列表组件
// 职责：展示当天所有活动场次，显示容量、已预约、已签到、候补人数
// 交互：点击场次选中，通过 EventBus 通知其他组件选中变化
// 接收：sessions 数组（由 App 组件通过 props 传入，来源为 WebSocket 推送）

interface SessionListProps {
  sessions: Session[];
  selectedId: string | null;
}

const SessionList: React.FC<SessionListProps> = ({ sessions, selectedId }) => {
  const handleSelect = (id: string) => {
    eventBus.emit(EVENTS.SESSION_SELECTED, id);
  };

  return (
    <div className="session-list">
      <h2>📋 今日场次</h2>
      <div className="session-items">
        {sessions.length === 0 && <p className="empty">暂无场次</p>}
        {sessions.map((session) => {
          const isFull = session.bookedCount >= session.capacity;
          const isSelected = selectedId === session.id;
          const remain = Math.max(0, session.capacity - session.bookedCount);

          return (
            <div
              key={session.id}
              className={`session-card ${isSelected ? 'selected' : ''} ${isFull ? 'full' : ''} ${
                session.status === 'closed' ? 'closed' : ''
              }`}
              onClick={() => handleSelect(session.id)}
            >
              <div className="session-header">
                <span className="session-name">
                  {session.name}
                  {session.status === 'closed' && <span className="badge-closed">已关闭</span>}
                </span>
                <span className="session-time">
                  {session.startTime} - {session.endTime}
                </span>
              </div>
              <div className="session-stats">
                <div className="stat">
                  <span className="stat-label">容量</span>
                  <span className="stat-value">{session.capacity}</span>
                </div>
                <div className="stat">
                  <span className="stat-label">剩余</span>
                  <span className={`stat-value ${remain === 0 ? 'full' : ''}`}>
                    {remain}
                  </span>
                </div>
                <div className="stat">
                  <span className="stat-label">已签到</span>
                  <span className="stat-value checked">{session.checkedInCount}</span>
                </div>
                {session.waitlistCount > 0 && (
                  <div className="stat">
                    <span className="stat-label">候补</span>
                    <span className="stat-value waitlist">{session.waitlistCount}</span>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <style>{`
        .session-list {
          background: white;
          border-radius: 8px;
          padding: 16px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
          height: 100%;
          overflow-y: auto;
        }
        .session-list h2 {
          font-size: 18px;
          color: #1a73e8;
          margin-bottom: 12px;
          border-bottom: 2px solid #e8f0fe;
          padding-bottom: 8px;
        }
        .session-items {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }
        .session-card {
          border: 2px solid #e0e0e0;
          border-radius: 8px;
          padding: 12px;
          cursor: pointer;
          transition: all 0.2s;
          background: #fafafa;
        }
        .session-card:hover {
          border-color: #1a73e8;
          background: #f0f7ff;
        }
        .session-card.selected {
          border-color: #1a73e8;
          background: #e8f0fe;
          box-shadow: 0 2px 8px rgba(26, 115, 232, 0.2);
        }
        .session-card.full {
          opacity: 0.8;
        }
        .session-card.closed {
          opacity: 0.5;
          background: #f1f3f4;
        }
        .session-card.closed:hover {
          border-color: #dadce0;
          background: #f1f3f4;
        }
        .badge-closed {
          margin-left: 6px;
          background: #d93025;
          color: white;
          font-size: 10px;
          padding: 2px 6px;
          border-radius: 3px;
          font-weight: normal;
        }
        .session-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;
        }
        .session-name {
          font-weight: 600;
          font-size: 15px;
          color: #202124;
        }
        .session-time {
          font-size: 13px;
          color: #5f6368;
          background: #f1f3f4;
          padding: 2px 8px;
          border-radius: 4px;
        }
        .session-stats {
          display: flex;
          gap: 12px;
          flex-wrap: wrap;
        }
        .stat {
          display: flex;
          flex-direction: column;
          align-items: center;
          min-width: 50px;
        }
        .stat-label {
          font-size: 11px;
          color: #5f6368;
        }
        .stat-value {
          font-size: 16px;
          font-weight: 600;
          color: #202124;
        }
        .stat-value.full {
          color: #d93025;
        }
        .stat-value.checked {
          color: #188038;
        }
        .stat-value.waitlist {
          color: #e37400;
        }
        .empty {
          text-align: center;
          color: #80868b;
          padding: 40px 0;
        }
      `}</style>
    </div>
  );
};

export default SessionList;
