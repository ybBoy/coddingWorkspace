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
      showNotification('msg queued');
    });
    const unsub5 = eventBus.on('SENDING_DISABLED', () => {
      showNotification('sending disabled by moderator');
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
      showNotification('sending disabled by moderator');
      return;
    }

    eventBus.emit('SEND_MESSAGE', {
      content: content.trim(),
      nickname: nickname.trim() || 'anonymous',
    });
    setContent('');
  };

  return (
    <div className="audience-panel">
      <div className="audience-header">
        <h1 className="audience-title">Live Danmaku Wall</h1>
        <div className="connection-status">
          <span className={`status-dot ${connected ? 'online' : 'offline'}`}></span>
          <span className="status-text">{connected ? 'Connected' : 'Connecting...'}</span>
        </div>
      </div>

      <div className="audience-body">
        <div className="info-card">
          <p>Send your message here, it will appear on the big screen after review!</p>
          <p className="hint">Sent: {sentCount}</p>
        </div>

        {!sendingEnabled && (
          <div className="disabled-notice">
            Moderator has paused danmaku sending
          </div>
        )}

        <form onSubmit={handleSubmit} className="send-form">
          <div className="form-group">
            <label>Nickname (optional)</label>
            <input
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="Anonymous"
              maxLength={20}
              className="nickname-input"
            />
          </div>

          <div className="form-group">
            <label>Your message</label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Send a danmaku~"
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
            Send Danmaku
          </button>
        </form>
      </div>

      {showToast && (
        <div className="toast">{showToast}</div>
      )}
    </div>
  );
};
