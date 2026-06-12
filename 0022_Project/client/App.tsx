import React, { useState, useEffect, useCallback, useRef } from 'react';
import EventBus from './EventBus';
import TaskBoard from './TaskBoard';
import TaskForm from './TaskForm';
import ActivityLog from './ActivityLog';
import { Task, TaskLog as TLog, WsMessage, PriorityFilter } from './types';

const WS_URL = `ws://localhost:8080/ws`;

function App() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [logs, setLogs] = useState<TLog[]>([]);
  const [nickname, setNickname] = useState<string>('');
  const [nicknameInput, setNicknameInput] = useState<string>('');
  const [connected, setConnected] = useState<boolean>(false);
  const [priorityFilter, setPriorityFilter] = useState<PriorityFilter>('all');
  const wsRef = useRef<WebSocket | null>(null);

  const sendWs = useCallback((data: object) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(data));
    }
  }, []);

  useEffect(() => {
    const ws = new WebSocket(WS_URL);
    wsRef.current = ws;

    ws.onopen = () => setConnected(true);
    ws.onclose = () => setConnected(false);
    ws.onerror = () => setConnected(false);

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data);
        setTasks(msg.tasks);
        setLogs(msg.logs);
        EventBus.emit('tasks-updated', msg.tasks);
        EventBus.emit('logs-updated', msg.logs);
      } catch (e) {
        console.error('Failed to parse ws message', e);
      }
    };

    return () => {
      ws.close();
    };
  }, []);

  const handleSetNickname = () => {
    const trimmed = nicknameInput.trim();
    if (trimmed) {
      setNickname(trimmed);
    }
  };

  const handleClaim = (taskId: string) => {
    sendWs({ action: 'claimTask', taskId, nickname });
  };

  const handleRelease = (taskId: string) => {
    sendWs({ action: 'releaseTask', taskId, nickname });
  };

  const handleComplete = (taskId: string) => {
    sendWs({ action: 'completeTask', taskId, nickname });
  };

  const handleAddTask = (title: string, description: string, priority: 'high' | 'medium' | 'low') => {
    sendWs({ action: 'addTask', task: { title, description, priority } });
  };

  if (!nickname) {
    return (
      <div className="app">
        <div className="nickname-modal">
          <div className="nickname-card">
            <h2>🚀 实时任务认领看板</h2>
            <p>请输入你的昵称加入协作</p>
            <div className="nickname-form">
              <input
                type="text"
                placeholder="输入昵称..."
                value={nicknameInput}
                onChange={e => setNicknameInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSetNickname()}
                maxLength={20}
              />
              <button onClick={handleSetNickname} disabled={!nicknameInput.trim()}>
                加入看板
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const filteredTasks = priorityFilter === 'all'
    ? tasks
    : tasks.filter(t => t.priority === priorityFilter);

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-left">
          <h1>📋 实时任务认领看板</h1>
        </div>
        <div className="header-center">
          <span className={`conn-status ${connected ? 'connected' : 'disconnected'}`}>
            {connected ? '🟢 已连接' : '🔴 未连接'}
          </span>
        </div>
        <div className="header-right">
          <span className="nickname-badge">👤 {nickname}</span>
        </div>
      </header>
      <div className="app-body">
        <div className="main-area">
          <div className="filter-bar">
            <span>筛选优先级：</span>
            {(['all', 'high', 'medium', 'low'] as PriorityFilter[]).map(p => (
              <button
                key={p}
                className={`filter-btn ${priorityFilter === p ? 'active' : ''} filter-${p}`}
                onClick={() => setPriorityFilter(p)}
              >
                {p === 'all' ? '全部' : p === 'high' ? '高' : p === 'medium' ? '中' : '低'}
              </button>
            ))}
          </div>
          <TaskBoard
            tasks={filteredTasks}
            nickname={nickname}
            onClaim={handleClaim}
            onRelease={handleRelease}
            onComplete={handleComplete}
          />
        </div>
        <aside className="sidebar">
          <TaskForm onAdd={handleAddTask} />
          <ActivityLog logs={logs} />
        </aside>
      </div>
    </div>
  );
}

export default App;
