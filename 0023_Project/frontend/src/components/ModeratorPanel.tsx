import React, { useState, useEffect } from 'react';
import { eventBus } from '../utils/EventBus';
import { DanmakuMessage, OperationLog, Settings } from '../types';
import { SettingsPanel } from './SettingsPanel';

type FilterMode = 'all' | 'sensitive' | 'normal';
type SubView = 'moderate' | 'approved' | 'settings' | 'logs';

export const ModeratorPanel: React.FC = () => {
  const [pendingMessages, setPendingMessages] = useState<DanmakuMessage[]>([]);
  const [approvedMessages, setApprovedMessages] = useState<DanmakuMessage[]>([]);
  const [settings, setSettings] = useState<Settings | null>(null);
  const [connected, setConnected] = useState(false);
  const [isModerator, setIsModerator] = useState(false);
  const [token, setToken] = useState(() => localStorage.getItem('danmaku_token') || '');
  const [password, setPassword] = useState('');
  const [authError, setAuthError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterMode, setFilterMode] = useState<FilterMode>('all');
  const [subView, setSubView] = useState<SubView>('moderate');
  const [logs, setLogs] = useState<OperationLog[]>([]);
  const [onlineCount, setOnlineCount] = useState(0);
  const [approvedSearch, setApprovedSearch] = useState('');

  useEffect(() => {
    const unsubs = [
      eventBus.on('PENDING_LIST', (list: DanmakuMessage[]) => { setPendingMessages(list); setIsModerator(true); }),
      eventBus.on('APPROVED_LIST', (list: DanmakuMessage[]) => { setApprovedMessages(list); setIsModerator(true); }),
      eventBus.on('NEW_PENDING', (msg: DanmakuMessage) => setPendingMessages(prev => [...prev, msg])),
      eventBus.on('PENDING_UPDATED', (msg: DanmakuMessage) => {
        setPendingMessages(prev => prev.filter(m => m.id !== msg.id));
      }),
      eventBus.on('NEW_MESSAGE', (msg: DanmakuMessage) => {
        setApprovedMessages(prev => [...prev, msg]);
      }),
      eventBus.on('PIN_UPDATED', (msg: DanmakuMessage) => {
        setApprovedMessages(prev => prev.map(m => m.id === msg.id ? msg : m));
      }),
      eventBus.on('SETTING_UPDATED', (data: any) => setSettings(data as Settings)),
      eventBus.on('WS_CONNECTED', () => setConnected(true)),
      eventBus.on('WS_DISCONNECTED', () => setConnected(false)),
      eventBus.on('AUTH_FAILED', () => { setIsModerator(false); setAuthError('Auth failed'); }),
      eventBus.on('AUTH_SUCCESS', (data: any) => {
        setIsModerator(true);
        const t = data?.token || '';
        setToken(t);
        localStorage.setItem('danmaku_token', t);
        eventBus.emit('GET_PENDING');
      }),
      eventBus.on('CLEAR_SCREEN', () => setApprovedMessages([])),
      eventBus.on('ONLINE_COUNT', (data: any) => setOnlineCount(data?.onlineCount || 0)),
      eventBus.on('OPERATION_LOGS', (data: OperationLog[]) => setLogs(data)),
      eventBus.on('EXPORT_DONE', (data: any) => alert('Exported to: ' + (data?.exportPath || 'server'))),
      eventBus.on('BACKUP_DONE', () => alert('Backup rotated!')),
    ];
    return () => unsubs.forEach(u => u());
  }, []);

  useEffect(() => {
    if (token) eventBus.emit('VALIDATE_TOKEN', { token });
  }, []);

  const sendingEnabled = settings?.sendingEnabled ?? true;
  const playbackPaused = settings?.playbackPaused ?? false;

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!password.trim()) return;
    setAuthError('');
    eventBus.emit('SET_ROLE', { role: 'moderator', password });
  };

  const handleApprove = (id: string) => eventBus.emit('APPROVE_MESSAGE', { id });
  const handleReject = (id: string) => eventBus.emit('REJECT_MESSAGE', { id });
  const handlePin = (id: string) => eventBus.emit('TOGGLE_PIN', { id });
  const handleApproveAndPin = (id: string) => eventBus.emit('APPROVE_AND_PIN', { id });

  const handleClearScreen = () => { if (window.confirm('Clear all danmaku?')) eventBus.emit('CLEAR_SCREEN'); };
  const handleToggleSending = () => eventBus.emit('TOGGLE_SENDING', { enabled: !sendingEnabled });
  const handleTogglePlayback = () => eventBus.emit('TOGGLE_PLAYBACK', { paused: !playbackPaused });
  const handleApproveNormalOnly = () => {
    if (window.confirm('Approve all normal (non-sensitive) messages?')) eventBus.emit('APPROVE_NORMAL_ONLY');
  };
  const handleApproveAll = () => {
    if (pendingMessages.length === 0) return;
    if (window.confirm('Approve all ' + pendingMessages.length + ' messages?'))
      pendingMessages.forEach(m => handleApprove(m.id));
  };
  const handleRejectAll = () => {
    if (pendingMessages.length === 0) return;
    if (window.confirm('Reject all ' + pendingMessages.length + ' messages?'))
      pendingMessages.forEach(m => handleReject(m.id));
  };

  const switchSubView = (v: SubView) => {
    setSubView(v);
    if (v === 'logs') eventBus.emit('GET_LOGS');
    if (v === 'approved') eventBus.emit('GET_APPROVED');
  };

  const filteredPending = [...pendingMessages]
    .filter(m => {
      if (filterMode === 'sensitive') return m.sensitive;
      if (filterMode === 'normal') return !m.sensitive;
      return true;
    })
    .filter(m => !searchQuery || m.content.toLowerCase().includes(searchQuery.toLowerCase()) || m.nickname.toLowerCase().includes(searchQuery.toLowerCase()))
    .sort((a, b) => {
      if (a.sensitive && !b.sensitive) return -1;
      if (!a.sensitive && b.sensitive) return 1;
      return a.timestamp - b.timestamp;
    });

  const filteredApproved = [...approvedMessages]
    .filter(m => !approvedSearch || m.content.toLowerCase().includes(approvedSearch.toLowerCase()) || m.nickname.toLowerCase().includes(approvedSearch.toLowerCase()))
    .sort((a, b) => {
      if (a.pinned && !b.pinned) return -1;
      if (!a.pinned && b.pinned) return 1;
      return b.timestamp - a.timestamp;
    })
    .slice(-100);

  const sensitiveCount = pendingMessages.filter(m => m.sensitive).length;
  const normalCount = pendingMessages.filter(m => !m.sensitive).length;

  if (!isModerator) {
    return (
      <div className="moderator-login">
        <div className="login-card">
          <h2>Moderator Login</h2>
          <form onSubmit={handleLogin}>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)}
              placeholder="Enter moderator password" className="password-input" />
            {authError && <p className="auth-error">{authError}</p>}
            <button type="submit" className="login-btn">Enter</button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="moderator-panel">
      <div className="moderator-header">
        <h2>Moderator Console</h2>
        <div className="header-actions">
          <span className={'conn-status ' + (connected ? 'ok' : 'bad')}>{connected ? 'Connected' : 'Disconnected'}</span>
          <span className="online-badge">{'Online: ' + onlineCount}</span>
          <span className="online-badge">{'Pending: ' + pendingMessages.length}</span>
        </div>
      </div>

      <div className="moderator-tabs">
        {(['moderate', 'approved', 'settings', 'logs'] as SubView[]).map(v => (
          <button key={v} className={'tab-btn' + (subView === v ? ' active' : '')} onClick={() => switchSubView(v)}>
            {v.charAt(0).toUpperCase() + v.slice(1)}
            {v === 'moderate' && pendingMessages.length > 0 ? ' (' + pendingMessages.length + ')' : ''}
          </button>
        ))}
      </div>

      {subView === 'settings' && settings && (
        <SettingsPanel settings={settings} token={token} />
      )}

      {subView === 'logs' && (
        <div className="logs-section">
          <h3>Operation Logs</h3>
          {logs.length === 0 ? <p className="empty-list">No logs yet</p> : (
            <div className="log-list">
              {logs.map((log, i) => (
                <div key={i} className="log-item">
                  <span className="log-time">{new Date(log.timestamp).toLocaleTimeString()}</span>
                  <span className="log-action">{'[' + log.action + ']'}</span>
                  <span className="log-detail">{log.detail}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {subView === 'approved' && (
        <div className="approved-section">
          <div className="control-bar">
            <input type="text" value={approvedSearch} onChange={e => setApprovedSearch(e.target.value)}
              placeholder="Search approved messages..." className="search-input" />
            <span className="hint">{'Last ' + filteredApproved.length + ' messages. Pinned first.'}</span>
          </div>
          {filteredApproved.length === 0 ? (
            <div className="empty-list"><p>No approved messages yet</p></div>
          ) : (
            <div className="message-list">
              {filteredApproved.map(msg => (
                <div key={msg.id} className={'message-card' + (msg.pinned ? ' pinned' : '')}>
                  <div className="message-header">
                    <span className="message-nickname" style={{ color: msg.color }}>{msg.nickname}</span>
                    {msg.pinned && <span className="sensitive-tag">Pinned</span>}
                    <span className="message-time">{new Date(msg.timestamp).toLocaleTimeString()}</span>
                  </div>
                  <div className="message-content">{msg.content}</div>
                  <div className="message-actions">
                    <button className={'action-btn ' + (msg.pinned ? 'pin-active' : 'pin')} onClick={() => handlePin(msg.id)}>
                      {msg.pinned ? 'Unpin' : 'Pin to Top'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {subView === 'moderate' && (
        <>
          <div className="control-bar">
            <button className={'control-btn ' + (sendingEnabled ? 'primary' : 'danger')} onClick={handleToggleSending}>
              {sendingEnabled ? 'Pause Sending' : 'Resume Sending'}
            </button>
            <button className={'control-btn ' + (playbackPaused ? 'primary' : 'warning')} onClick={handleTogglePlayback}>
              {playbackPaused ? 'Resume Playback' : 'Pause Playback'}
            </button>
            <button className="control-btn warning" onClick={handleClearScreen}>Clear Screen</button>
            <div className="stats-info">
              <span>{'Pending: '}<strong>{pendingMessages.length}</strong></span>
              {sensitiveCount > 0 && <span className="sensitive-badge">{'Sensitive: ' + sensitiveCount}</span>}
            </div>
          </div>

          <div className="batch-actions">
            <button className="batch-btn approve-all" onClick={handleApproveAll}>Approve All</button>
            <button className="batch-btn approve-normal" onClick={handleApproveNormalOnly}>Approve Normal Only</button>
            <button className="batch-btn reject-all" onClick={handleRejectAll}>Reject All</button>
          </div>

          <div className="filter-bar">
            <input type="text" value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
              placeholder="Search by keyword..." className="search-input" />
            <div className="filter-btns">
              {(['all', 'sensitive', 'normal'] as FilterMode[]).map(f => (
                <button key={f} className={'filter-btn' + (filterMode === f ? ' active' : '')}
                  onClick={() => setFilterMode(f)}>
                  {f === 'all' ? 'All' + ' (' + pendingMessages.length + ')' : f === 'sensitive' ? 'Sensitive' + ' (' + sensitiveCount + ')' : 'Normal' + ' (' + normalCount + ')'}
                </button>
              ))}
            </div>
          </div>

          <div className="pending-list">
            {filteredPending.length === 0 ? (
              <div className="empty-list"><p>No pending messages</p></div>
            ) : (
              <div className="message-list">
                {filteredPending.map(msg => (
                  <div key={msg.id} className={'message-card' + (msg.sensitive ? ' sensitive' : '')}>
                    <div className="message-header">
                      <span className="message-nickname" style={{ color: msg.color }}>{msg.nickname}</span>
                      {msg.sensitive && <span className="sensitive-tag">Needs Review</span>}
                      <span className="message-time">{new Date(msg.timestamp).toLocaleTimeString()}</span>
                    </div>
                    <div className="message-content">{msg.content}</div>
                    <div className="message-actions">
                      <button className="action-btn approve" onClick={() => handleApprove(msg.id)}>Approve</button>
                      <button className="action-btn pin" onClick={() => handleApproveAndPin(msg.id)}>Approve & Pin</button>
                      <button className="action-btn reject" onClick={() => handleReject(msg.id)}>Reject</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};
