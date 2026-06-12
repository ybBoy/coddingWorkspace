import React, { useState, useEffect } from 'react';
import { eventBus } from '../utils/EventBus';

export const AudiencePanel: React.FC = () => {
  const [content, setContent] = useState('');
  const [nickname, setNickname] = useState('');
  const [sendingEnabled, setSendingEnabled] = useState(true);
  const [connected, setConnected] = useState(false);
  const [sentCount, setSentCount] = useState(0);
  const [showToast, setShowToast] = useState('');

  useEffect(() => {
    const unsub1 = eventBus.on('SETTING_UPDATED', (data: any) => {
      setSendingEnabled(data?.sendingEnabled ?? true);
    });
    const unsub2 = eventBus.on('WS_CONNECTED', () => setConnected(true));
    const unsub3 = eventBus.on('WS_DISCONNECTED', () => setConnected(false));
    const unsub4 = eventBus.on('MESSAGE_QUEUED', () => {
      setSentCount(c => c + 1);
      showNotification('消息已提交，等待审核~');
    });
    const unsub5 = eventBus.on('SENDING_DISABLED', () => {
      showNotification('主持人暂时关闭了发送功能');
    });

    return () => {
      unsub1();
      unsub2();
      unsub3();
      unsub4();
      unsub5();
    };
  }, []);

  const showNotification = (msg: string) => {
    setShowToast(msg);
    setTimeout(() => setShowToast(''), 2500);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;
    if (!sendingEnabled) {
      showNotification('主持人暂时关闭了发送功能');
      return;
    }

    eventBus.emit('SEND_MESSAGE', {
      content: content.trim(),
      nickname: nickname.trim() || '匿名',
    });
    setContent('');
  };

  return (
    <div className="audience-panel">
      <div className="audience-header">
        <h1 className="audience-title">📢 现场弹幕墙</h1>
        <div className="connection-status">
          <span className={`status-dot ${connected ? 'online' : 'offline'}`}></span>
          <span className="status-text">{connected ? '已连接' : '连接中...'}</span>
        </div>
      </div>

      <div className="audience-body">
        <div className="info-card">
          <p>在这里发送你的想法，通过审核后会在大屏幕上显示~</p>
          <p className="hint">已发送 {sentCount} 条消息</p>
        </div>

        {!sendingEnabled && (
          <div className="disabled-notice">
            ⚠️ 主持人暂时关闭了弹幕发送
          </div>
        )}

        <form onSubmit={handleSubmit} className="send-form">
          <div className="form-group">
            <label>你的昵称（可选）</label>
            <input
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="匿名用户"
              maxLength={20}
              className="nickname-input"
            />
          </div>

          <div className="form-group">
            <label>想说的话</label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="发送一条弹幕吧~"
              maxLength={100}
              rows={4}
              className="content-input"
              disabled={!sendingEnabled}
            />
            <div className="char-count">{content.length}/100</div>
          </div>

          <button
            type="submit"
            className="send-btn"
            disabled={!content.trim() || !sendingEnabled || !connected}
          >
            🚀 发送弹幕
          </button>
        </form>
      </div>

      {showToast && (
        <div className="toast">{showToast}</div>
      )}
    </div>
  );
};
