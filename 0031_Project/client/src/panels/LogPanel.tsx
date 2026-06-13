import type { RoomLog } from '../base/types';
import './LogPanel.css';

interface LogPanelProps {
  logs: RoomLog[];
}

function formatTime(timestamp: number): string {
  const date = new Date(timestamp);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function getActionColor(action: string): string {
  if (action.includes('入住') || action.includes('check in') || action.includes('CHECK_IN')) {
    return 'action-checkin';
  }
  if (action.includes('退房') || action.includes('check out') || action.includes('CHECK_OUT')) {
    return 'action-checkout';
  }
  if (action.includes('打扫') || action.includes('CLEAN') || action.includes('clean')) {
    return 'action-clean';
  }
  if (action.includes('维修') || action.includes('报修') || action.includes('MAINTENANCE') || action.includes('REPAIR')) {
    return 'action-maintenance';
  }
  return '';
}

export function LogPanel({ logs }: LogPanelProps) {
  const recentLogs = logs.slice(-10).reverse();

  return (
    <div className="log-panel">
      <div className="log-header">
        <span className="log-title">操作日志（最近 10 条）</span>
      </div>
      <div className="log-list">
        {recentLogs.length === 0 ? (
          <div className="log-empty">暂无操作日志</div>
        ) : (
          recentLogs.map((log) => (
            <div key={log.id} className="log-item">
              <span className="log-time">[{formatTime(log.timestamp)}]</span>
              <span className="log-room">{log.roomNo}</span>
              <span className={`log-action ${getActionColor(log.action)}`}>{log.action}</span>
              {log.remark && <span className="log-remark">— {log.remark}</span>}
              <span className="log-operator">（{log.operator}）</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
