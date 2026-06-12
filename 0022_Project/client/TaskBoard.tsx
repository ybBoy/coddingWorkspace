import React from 'react';
import TaskCard from './TaskCard';
import { Task } from './types';

interface TaskBoardProps {
  tasks: Task[];
  nickname: string;
  onClaim: (id: string) => void;
  onRelease: (id: string) => void;
  onComplete: (id: string) => void;
}

function TaskBoard({ tasks, nickname, onClaim, onRelease, onComplete }: TaskBoardProps) {
  const pending = tasks.filter(t => t.status === 'pending');
  const inProgress = tasks.filter(t => t.status === 'in_progress');
  const completed = tasks.filter(t => t.status === 'completed');

  const renderColumn = (title: string, icon: string, items: Task[]) => (
    <div className="board-column">
      <div className="column-header">
        <span className="column-icon">{icon}</span>
        <h3>{title}</h3>
        <span className="column-count">{items.length}</span>
      </div>
      <div className="column-body">
        {items.length === 0 ? (
          <div className="empty-hint">暂无任务</div>
        ) : (
          items.map(task => (
            <TaskCard
              key={task.id}
              task={task}
              nickname={nickname}
              onClaim={onClaim}
              onRelease={onRelease}
              onComplete={onComplete}
            />
          ))
        )}
      </div>
    </div>
  );

  return (
    <div className="task-board">
      {renderColumn('待认领', '📌', pending)}
      {renderColumn('进行中', '🔄', inProgress)}
      {renderColumn('已完成', '✅', completed)}
    </div>
  );
}

export default TaskBoard;
