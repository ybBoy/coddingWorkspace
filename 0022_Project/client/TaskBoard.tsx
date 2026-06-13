import React, { useState, useEffect } from 'react';
import TaskCard from './TaskCard';
import EventBus from './EventBus';
import { Task, PriorityFilter } from './types';

interface TaskBoardProps {
  onClaim: (id: string) => void;
  onStart: (id: string) => void;
  onRelease: (id: string) => void;
  onComplete: (id: string) => void;
}

function TaskBoard({ onClaim, onStart, onRelease, onComplete }: TaskBoardProps) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [nickname, setNickname] = useState<string>('');
  const [priorityFilter, setPriorityFilter] = useState<PriorityFilter>('all');

  useEffect(() => {
    const off1 = EventBus.on('tasks-updated', (newTasks: Task[]) => setTasks(newTasks));
    const off2 = EventBus.on('set-nickname', (n: string) => setNickname(n));
    const off3 = EventBus.on('set-priority-filter', (p: PriorityFilter) => setPriorityFilter(p));
    return () => { off1(); off2(); off3(); };
  }, []);

  const filtered = priorityFilter === 'all'
    ? tasks
    : tasks.filter(t => t.priority === priorityFilter);

  const pending = filtered.filter(t => t.status === 'pending');
  const claimed = filtered.filter(t => t.status === 'claimed');
  const inProgress = filtered.filter(t => t.status === 'in_progress');
  const completed = filtered.filter(t => t.status === 'completed');

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
              onStart={onStart}
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
      {renderColumn('已认领', '🙋', claimed)}
      {renderColumn('进行中', '🔄', inProgress)}
      {renderColumn('已完成', '✅', completed)}
    </div>
  );
}

export default TaskBoard;
