import React, { useState, useEffect } from 'react';
import { Task } from './types';

const TIMEOUT_MS = 30 * 60 * 1000;

interface TaskCardProps {
  task: Task;
  nickname: string;
  onClaim: (id: string) => void;
  onStart: (id: string) => void;
  onRelease: (id: string) => void;
  onComplete: (id: string) => void;
}

function TaskCard({ task, nickname, onClaim, onStart, onRelease, onComplete }: TaskCardProps) {
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 15000);
    return () => clearInterval(timer);
  }, []);

  const isTimeout = (task.status === 'claimed' || task.status === 'in_progress')
    && task.claimedAt > 0
    && now - task.claimedAt > TIMEOUT_MS;

  const isAssignee = task.assignee === nickname;

  const priorityLabel = task.priority === 'high' ? '高' : task.priority === 'medium' ? '中' : '低';
  const priorityClass = `priority-${task.priority}`;

  const statusLabel = task.status === 'pending'
    ? '待认领'
    : task.status === 'claimed'
    ? '已认领'
    : task.status === 'in_progress'
    ? '进行中'
    : '已完成';

  return (
    <div className={`task-card status-${task.status}`}>
      <div className="task-card-header">
        <span className={`priority-tag ${priorityClass}`}>{priorityLabel}</span>
        <span className="status-text">{statusLabel}</span>
        {isTimeout && <span className="timeout-warning">⚠ 即将超时</span>}
      </div>
      <h4 className="task-title">{task.title}</h4>
      {task.description && <p className="task-desc">{task.description}</p>}
      {task.assignee && (
        <div className="task-assignee">👤 {task.assignee}</div>
      )}
      <div className="task-actions">
        {task.status === 'pending' && (
          <button className="btn btn-primary btn-sm" onClick={() => onClaim(task.id)}>
            认领任务
          </button>
        )}
        {isAssignee && task.status === 'claimed' && (
          <>
            <button className="btn btn-warning btn-sm" onClick={() => onRelease(task.id)}>
              释放
            </button>
            <button className="btn btn-primary btn-sm" onClick={() => onStart(task.id)}>
              开始进行
            </button>
          </>
        )}
        {isAssignee && task.status === 'in_progress' && (
          <>
            <button className="btn btn-warning btn-sm" onClick={() => onRelease(task.id)}>
              释放
            </button>
            <button className="btn btn-success btn-sm" onClick={() => onComplete(task.id)}>
              完成
            </button>
          </>
        )}
      </div>
    </div>
  );
}

export default TaskCard;
