import React, { useState, useEffect } from 'react';
import { eventBus } from '../utils/EventBus';
import { DanmakuMessage } from '../types';

export const ModeratorPanel: React.FC = () => {
  const [pendingMessages, setPendingMessages] = useState<DanmakuMessage[]>([]);
  const [sendingEnabled, setSendingEnabled] = useState(true);
  const [connected, setConnected] = useState(false);
  const [isModerator, setIsModerator] = useState(false);
  const [password, setPassword] = useState('');
  const [authError, setAuthError] = useState('');

  useEffect(() => {
    const unsub1 = eventBus.on('PENDING_LIST', (list: DanmakuMessage[]) => {
      setPendingMessages(list);
      setIsModerator(true);
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
    const unsub7 = eventBus.on('AUTH_FAILED', () => {
      setIsModerator(false);
      setAuthError('Auth failed, please try again');
    });

    return () => {
      unsub1();
      unsub2();
      unsub3();
      unsub4();
      unsub5();
      unsub6();
      unsub7();
    };
  }, []);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!password.trim()) return;
    setAuthError('');
    eventBus.emit('SET_ROLE', { role: 'moderator', password: password });
  };

  const handleApprove = (id: string) => {
    eventBus.emit('APPROVE_MESSAGE', { id: id });
  };

  const handleReject = (id: string) => {
    eventBus.emit('REJECT_MESSAGE', { id: id });
  };

  const handleClearScreen = () => {
    if (window.confirm('Clear all danmaku on screen?')) {
      eventBus.emit('CLEAR_SCREEN');
    }
  };

  const handleToggleSending = () => {
    eventBus.emit('TOGGLE_SENDING', { enabled: !sendingEnabled });
  };

  const approveAll = () => {
    if (pendingMessages.length === 0) return;
    if (window.confirm('Approve all ' + pendingMessages.length + ' messages?')) {
      pendingMessages.forEach(msg => {
        eventBus.emit('APPROVE_MESSAGE', { id: msg.id });
      });
    }
  };

  const rejectAll = () => {
    if (pendingMessages.length === 0) return;
    if (window.confirm('Reject all ' + pendingMessages.length + ' messages?')) {
      pendingMessages.forEach(msg => {
        eventBus.emit('REJECT_MESSAGE', { id: msg.id });
      });
    }
  };

  if (!isModerator) {
    return (
      <div className="moderator-login">
        <div className="login-card">
          <h2>Moderator Login</h2>
          <form onSubmit={handleLogin}>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter moderator password"
              className="password-input"
            />
            {authError && <p className="auth-error">{authError}</p>}
            <button type="submit" className="login-btn">Enter</button>
          </form>
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
        <h2>Moderator Console</h2>
        <div className="header-actions">
          <span className={'conn-status ' + (connected ? 'ok' : 'bad')}>
            {connected ? 'Connected' : 'Disconnected'}
          </span>
        </div>
      </div>

      <div className="control-bar">
        <button
          className={'control-btn ' + (sendingEnabled ? 'primary' : 'danger')}
          onClick={handleToggleSending}
        >
          {sendingEnabled ? 'Pause Sending' : 'Resume Sending'}
        </button>
        <button className="control-btn warning" onClick={handleClearScreen}>
          Clear Screen
        </button>
        <div className="stats-info">
          <span>{'Pending: '}<strong>{pendingMessages.length}</strong></span>
          {sensitiveCount > 0 && (
            <span className="sensitive-badge">{'Sensitive: ' + sensitiveCount}</span>
          )}
        </div>
      </div>

      <div className="batch-actions">
        <button className="batch-btn approve-all" onClick={approveAll}>
          Approve All
        </button>
        <button className="batch-btn reject-all" onClick={rejectAll}>
          Reject All
        </button>
      </div>

      <div className="pending-list">
        <h3>Pending Messages</h3>
        {sortedPending.length === 0 ? (
          <div className="empty-list">
            <p>No pending messages</p>
          </div>
        ) : (
          <div className="message-list">
            {sortedPending.map(msg => (
              <div
                key={msg.id}
                className={'message-card' + (msg.sensitive ? ' sensitive' : '')}
              >
                <div className="message-header">
                  <span className="message-nickname" style={{ color: msg.color }}>
                    {msg.nickname}
                  </span>
                  {msg.sensitive && (
                    <span className="sensitive-tag">Needs Review</span>
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
                    Approve
                  </button>
                  <button
                    className="action-btn reject"
                    onClick={() => handleReject(msg.id)}
                  >
                    Reject
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
