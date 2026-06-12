import React, { useState } from 'react';

interface TaskFormProps {
  onAdd: (title: string, description: string, priority: 'high' | 'medium' | 'low') => void;
}

function TaskForm({ onAdd }: TaskFormProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<'high' | 'medium' | 'low'>('medium');
  const [expanded, setExpanded] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    onAdd(title.trim(), description.trim(), priority);
    setTitle('');
    setDescription('');
    setPriority('medium');
    setExpanded(false);
  };

  return (
    <div className="task-form-panel">
      <div className="panel-header" onClick={() => setExpanded(!expanded)}>
        <h3>➕ 新增任务</h3>
        <span className="toggle-icon">{expanded ? '▲' : '▼'}</span>
      </div>
      {expanded && (
        <form onSubmit={handleSubmit} className="task-form">
          <input
            type="text"
            placeholder="任务标题 *"
            value={title}
            onChange={e => setTitle(e.target.value)}
            maxLength={50}
            required
          />
          <textarea
            placeholder="任务说明（可选）"
            value={description}
            onChange={e => setDescription(e.target.value)}
            maxLength={200}
            rows={3}
          />
          <div className="priority-select">
            <span>优先级：</span>
            {(['high', 'medium', 'low'] as const).map(p => (
              <button
                type="button"
                key={p}
                className={`priority-btn priority-${p} ${priority === p ? 'selected' : ''}`}
                onClick={() => setPriority(p)}
              >
                {p === 'high' ? '高' : p === 'medium' ? '中' : '低'}
              </button>
            ))}
          </div>
          <button type="submit" className="btn btn-primary" disabled={!title.trim()}>
            添加任务
          </button>
        </form>
      )}
    </div>
  );
}

export default TaskForm;
