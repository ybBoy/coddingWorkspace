import React, { useState, useEffect } from 'react';
import { eventBus } from '../utils/EventBus';
import { DanmakuMessage } from '../types';

export const ModeratorPanel: React.FC = () => {
  const [pendingMessages, setPendingMessages] = useState<DanmakuMessage[]>([]);
  const [sendingEnabled, setSendingEnabled] = useState(true);
  const [connected, setConnected] = useState(false);
  const [isModerator, setIsModerator] = useState(false);
  const [password, setPassword] = useState('');

  useEffect(() => {
    const unsub1 = eventBus.on('PENDING_LIST', (list: DanmakuMessage[]) => {
      setPendingMessages(list);
    });
    const unsub2 = eventBus.on('NEW_PENDING', (msg: DanmakuMessage) => {
      setPendingMessages(prev => [...prev, msg]);
    });
    const unsub3 = eventBus.on('PENDING_UPDATED', (msg: DanmakuMessage) => {
      setPendingMessages(prev => prev.filter(m => m.id !== msg.id));
    });
    const unsub4 = eventBus.on('SETTING_UPDATED', (data: any) => {
      setSendingEnabled(data?.sendingEnabled ?? true);
    });
    const unsub5 = eventBus.on('WS_CONNECTED', () => setConnected(true));
    const unsub6 = eventBus.on('WS_DISCONNECTED', () => setConnected(false));

    return () => {
      unsub1();
      unsub2();
      unsub3();
      unsub4();
      unsub5();
      unsub6();
    };
  }, []);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (password === 'admin123') {
      setIsModerator(true);
      eventBus.emit('SET_ROLE', { role: 'moderator' });
      setTimeout(() => {
        eventBus.emit('GET_PENDING');
      }, 100);
    } else {
      alert('密码错误');
    }
  };

  const handleApprove = (id: string) => {
    eventBus.emit('APPROVE_MESSAGE', { id });
  };

  const handleReject = (id: string) => {
    eventBus.emit('REJECT_MESSAGE', { id });
  };

  const handleClearScreen = () => {
    if (confirm('确定要清空当前屏幕所有弹幕吗？')) {
      eventBus.emit('CLEAR_SCREEN');
    }
  };

  const handleToggleSending = () => {
    eventBus.emit('TOGGLE_SENDING', { enabled: !sendingEnabled });
  };

  const approveAll = () => {
    if (pendingMessages.length === 0) return;
    if (confirm(`确定要通过全部 ${pendingMessages.length} 条消息吗？`)) {
      pendingMessages.forEach(msg => {
        eventBus.emit('APPROVE_MESSAGE', { id: msg.id });
      });
    }
  };

  const rejectAll = () => {
    if (pendingMessages.length === 0) return;
    if (confirm(`确定要拒绝全部 ${pendingMessages.length} 条消息吗？`)) {
      pendingMessages.forEach(msg => {
        eventBus.emit('REJECT_MESSAGE', { id: msg.id });
      });
    }
  };

  if (!isModerator) {
    return (
      <div className="moderator-login">
        <div className="login-card">
          <h2>🔐 主持人登录</h2>
          <form onSubmit={handleLogin}>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="请输入主持人密码"
              className="password-input"
            />
            <button type="submit" className="login-btn">进入管理面板</button>
          </form>
          <p className="hint">默认密码: admin123</p>
        </div>
      </div>
    );
  }

  const sortedPending = [...pendingMessages].sort((a, b) => {
    if (a.sensitive && !b.sensitive) return -1;
    if (!a.sensitive && b.sensitive) return 1;
    return a.timestamp - b.timestamp;
  });

  const sensitiveCount = pendingMessages.filter(m => m.sensitive).length;

  return (
    <div className="moderator-panel">
      <div className="moderator-header">
        <h2>🎛️ 主持人控制台</h2>
        <div className="header-actions">
          <span className={`conn-status ${connected ? 'ok' : 'bad'}`}>
            {connected ? '● 已连接' : '○ 未连接'}
          </span>
        </div>
      </div>

      <div className="control-bar">
        <button
          className={`control-btn ${sendingEnabled ? 'primary' : 'danger'}`}
          onClick={handleToggleSending}
        >
          {sendingEnabled ? '⏸️ 暂停发送' : '▶️ 恢复发送'}
        </button>
        <button className="control-btn warning" onClick={handleClearScreen}>
          🗑️ 清空屏幕
        </button>
        <div className="stats-info">
          <span>待审核: <strong>{pendingMessages.length}</strong></span>
          {sensitiveCount > 0 && (
            <span className="sensitive-badge">⚠️ 敏感: {sensitiveCount}</span>
          )}
        </div>
      </div>

      <div className="batch-actions">
        <button className="batch-btn approve-all" onClick={approveAll}>
          ✅ 全部通过
        </button>
        <button className="batch-btn reject-all" onClick={rejectAll}>
          ❌ 全部拒绝
        </button>
      </div>

      <div className="pending-list">
        <h3>待审核消息</h3>
        {sortedPending.length === 0 ? (
          <div className="empty-list">
            <p>暂无待审核消息</p>
          </div>
        ) : (
          <div className="message-list">
            {sortedPending.map(msg => (
              <div
                key={msg.id}
                className={`message-card ${msg.sensitive ? 'sensitive' : ''}`}
              >
                <div className="message-header">
                  <span className="message-nickname" style={{ color: msg.color }}>
                    {msg.nickname}
                  </span>
                  {msg.sensitive && (
                    <span className="sensitive-tag">⚠️ 需重点审核</span>
                  )}
                  <span className="message-time">
                    {new Date(msg.timestamp).toLocaleTimeString()}
                  </span>
                </div>
                <div className="message-content">{msg.content}</div>
                <div className="message-actions">
                  <button
                    className="action-btn approve"
                    onClick={() => handleApprove(msg.id)}
                  >
                    ✅ 通过
                  </button>
                  <button
                    className="action-btn reject"
                    onClick={() => handleReject(msg.id)}
                  >
                    ❌ 拒绝
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
