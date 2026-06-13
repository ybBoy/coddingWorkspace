import React from 'react';
import { SeatActionData } from '../core/EventBus';

interface ActivityFeedProps {
  actions: SeatActionData[];
}

const ACTION_LABEL: Record<string, string> = {
  sit: '入座',
  away: '暂离',
  leave: '离开',
  forceRelease: '被强制释放',
};

const ACTION_ICON: Record<string, string> = {
  sit: '🪑',
  away: '⏸',
  leave: '👋',
  forceRelease: '🔓',
};

const ActivityFeed: React.FC<ActivityFeedProps> = ({ actions }) => {
  return (
    <div className="activity-feed">
      <h3>最近动态</h3>
      {actions.length === 0 ? (
        <p className="no-activity">暂无动态</p>
      ) : (
        <ul className="activity-list">
          {actions.slice().reverse().map((action, idx) => {
            const time = new Date(action.timestamp);
            const timeStr = time.toLocaleTimeString('zh-CN', {
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit',
            });
            return (
              <li key={`${action.timestamp}-${idx}`} className="activity-item">
                <span className="activity-icon">{ACTION_ICON[action.action] || '📌'}</span>
                <span className="activity-text">
                  <strong>{action.nickname}</strong>
                  {' '}在 {action.seatId}号座位 {ACTION_LABEL[action.action] || action.action}
                </span>
                <span className="activity-time">{timeStr}</span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
};

export default ActivityFeed;
