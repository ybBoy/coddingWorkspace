import React, { useState, useEffect } from 'react';
import { eventBus } from '../utils/EventBus';
import { Settings } from '../types';

const NICKNAME_KEY = 'danmaku_nickname';

export const AudiencePanel: React.FC = () => {
  const [content, setContent] = useState('');
  const [nickname, setNickname] = useState(() => localStorage.getItem(NICKNAME_KEY) || '');
  const [settings, setSettings] = useState<Settings | null>(null);
  const [connected, setConnected] = useState(false);
  const [sentCount, setSentCount] = useState(0);
  const [showToast, setShowToast] = useState('');
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    const unsub1 = eventBus.on('SETTING_UPDATED', (data: any) => setSettings(data as Settings));
    const unsub2 = eventBus.on('WS_CONNECTED', () => setConnected(true));
    const unsub3 = eventBus.on('WS_DISCONNECTED', () => setConnected(false));
    const unsub4 = eventBus.on('MESSAGE_QUEUED', () => {
      setSentCount(c => c + 1);
      notify('Message queued for review!');
    });
    const unsub5 = eventBus.on('SENDING_DISABLED', () => notify('Sending disabled by moderator'));
    const unsub6 = eventBus.on('SEND_REJECTED', (data: any) => notify(data?.reason || 'Message rejected'));
    return () => { unsub1(); unsub2(); unsub3(); unsub4(); unsub5(); unsub6(); };
  }, []);

  useEffect(() => {
    if (nickname) localStorage.setItem(NICKNAME_KEY, nickname);
  }, [nickname]);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = window.setTimeout(() => setCooldown(c => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  const notify = (msg: string) => { setShowToast(msg); setTimeout(() => setShowToast(''), 2500); };

  const sendingEnabled = settings?.sendingEnabled ?? true;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim() || cooldown > 0) return;
    if (!sendingEnabled) { notify('Sending disabled by moderator'); return; }
    const nick = nickname.trim() || 'anonymous';
    eventBus.emit('SEND_MESSAGE', { content: content.trim(), nickname: nick });
    setContent('');
    setCooldown(3);
  };

  return (
    <div className="audience-panel">
      <div className="audience-header">
        <h1 className="audience-title">{settings?.eventTitle || 'Live Danmaku Wall'}</h1>
        <div className="connection-status">
          <span className={'status-dot ' + (connected ? 'online' : 'offline')}></span>
          <span className="status-text">{connected ? 'Connected' : 'Connecting...'}</span>
        </div>
      </div>

      <div className="audience-body">
        <div className="info-card">
          <p>{settings?.welcomeMessage || 'Send your message here!'}</p>
          <p className="hint">{'Sent: ' + sentCount}</p>
        </div>

        {!sendingEnabled && <div className="disabled-notice">Moderator has paused danmaku sending</div>}
        {cooldown > 0 && <div className="cooldown-notice">{'Cooldown: ' + cooldown + 's'}</div>}

        <form onSubmit={handleSubmit} className="send-form">
          <div className="form-group">
            <label>Nickname (saved locally)</label>
            <input type="text" value={nickname} onChange={e => setNickname(e.target.value)}
              placeholder="Anonymous" maxLength={20} className="nickname-input" />
          </div>
          <div className="form-group">
            <label>Your message</label>
            <textarea value={content} onChange={e => setContent(e.target.value)}
              placeholder="Send a danmaku~" maxLength={100} rows={4}
              className="content-input" disabled={!sendingEnabled} />
            <div className="char-count">{content.length + '/100'}</div>
          </div>
          <button type="submit" className="send-btn"
            disabled={!content.trim() || !sendingEnabled || !connected || cooldown > 0}>
            Send Danmaku
          </button>
        </form>
      </div>

      {showToast && <div className="toast">{showToast}</div>}
    </div>
  );
};
