import React, { useState, useEffect } from 'react';
import EventBus from './EventBus';
import { TaskLog as TLog } from './types';

const actionLabels: Record<string, string> = {
  created: '🆕 创建了',
  claimed: '🙋 认领了',
  started: '▶️ 开始进行',
  released: '↩️ 释放了',
  completed: '🎉 完成了',
};

function formatTime(ts: number): string {
  const d = new Date(ts);
  const h = d.getHours().toString().padStart(2, '0');
  const m = d.getMinutes().toString().padStart(2, '0');
  const s = d.getSeconds().toString().padStart(2, '0');
  return `${h}:${m}:${s}`;
}

function ActivityLog() {
  const [logs, setLogs] = useState<TLog[]>([]);

  useEffect(() => {
    const off = EventBus.on('logs-updated', (newLogs: TLog[]) => setLogs(newLogs));
    return off;
  }, []);

  return (
    <div className="activity-log-panel">
      <h3>📜 动态日志</h3>
      <div className="log-list">
        {logs.length === 0 ? (
          <div className="empty-hint">暂无动态</div>
        ) : (
          logs.map(log => (
            <div key={log.id} className="log-item">
              <span className="log-time">{formatTime(log.timestamp)}</span>
              <span className="log-content">
                <strong>{log.nickname || '系统'}</strong>
                {' '}{actionLabels[log.action] || log.action}{' '}
                <em>{log.taskTitle}</em>
              </span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default ActivityLog;
