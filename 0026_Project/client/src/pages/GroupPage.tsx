import React, { useState, useEffect } from 'react';
import { eventBus } from '../shared/EventBus';
import socket from '../shared/socket';
import { AppState, ConnectionStatus } from '../shared/types';
import ParticipantInput from '../components/ParticipantInput';
import GroupBoard from '../components/GroupBoard';
import HostControls from '../components/HostControls';
import '../styles/GroupPage.css';

const GroupPage: React.FC = () => {
  const [state, setState] = useState<AppState>({
    activityName: '活动抽签分组',
    groupCount: 4,
    participants: [],
    groups: [],
    logs: [],
    isHost: false,
  });
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('connecting');
  const [editingName, setEditingName] = useState(false);
  const [nameInput, setNameInput] = useState('');
  const [notification, setNotification] = useState<{ type: string; message: string } | null>(null);

  useEffect(() => {
    const handleStateUpdate = (data: AppState) => {
      setState(data);
    };

    const handleConnectionStatus = (status: ConnectionStatus) => {
      setConnectionStatus(status);
    };

    const handleHostGranted = () => {
      showNotification('success', '已登录为主持人');
      setState((prev) => ({ ...prev, isHost: true }));
    };

    const handleError = (message: string) => {
      showNotification('error', message);
    };

    const handleSuccess = (message: string) => {
      showNotification('success', message);
    };

    eventBus.on('state-update', handleStateUpdate);
    eventBus.on('connection-status', handleConnectionStatus);
    eventBus.on('host-granted', handleHostGranted);
    eventBus.on('error', handleError);
    eventBus.on('success', handleSuccess);

    socket.connect();

    return () => {
      eventBus.off('state-update', handleStateUpdate);
      eventBus.off('connection-status', handleConnectionStatus);
      eventBus.off('host-granted', handleHostGranted);
      eventBus.off('error', handleError);
      eventBus.off('success', handleSuccess);
      socket.disconnect();
    };
  }, []);

  const showNotification = (type: string, message: string) => {
    setNotification({ type, message });
    setTimeout(() => {
      setNotification(null);
    }, 3000);
  };

  const handleClaimHost = (token: string) => {
    socket.claimHost(token);
  };

  const handleEditName = () => {
    if (!state.isHost) return;
    setNameInput(state.activityName);
    setEditingName(true);
  };

  const handleSaveName = () => {
    if (nameInput.trim() && state.isHost) {
      socket.send({ type: 'set-activity-name', name: nameInput.trim() });
    }
    setEditingName(false);
  };

  const getStatusText = () => {
    switch (connectionStatus) {
      case 'connected':
        return '已连接';
      case 'connecting':
        return '连接中...';
      case 'disconnected':
        return '已断开';
      default:
        return '';
    }
  };

  const getStatusColor = () => {
    switch (connectionStatus) {
      case 'connected':
        return '#22c55e';
      case 'connecting':
        return '#f59e0b';
      case 'disconnected':
        return '#ef4444';
      default:
        return '#9ca3af';
    }
  };

  return (
    <div className="group-page">
      {notification && (
        <div className={`notification ${notification.type}`}>
          {notification.message}
        </div>
      )}

      <header className="app-header">
        <div className="header-left">
          {editingName ? (
            <div className="name-edit">
              <input
                type="text"
                value={nameInput}
                onChange={(e) => setNameInput(e.target.value)}
                onBlur={handleSaveName}
                onKeyDown={(e) => e.key === 'Enter' && handleSaveName()}
                autoFocus
              />
            </div>
          ) : (
            <h1
              className={state.isHost ? 'editable' : ''}
              onClick={handleEditName}
              title={state.isHost ? '点击修改活动名称' : ''}
            >
              {state.activityName}
              {state.isHost && <span className="edit-icon">✏️</span>}
            </h1>
          )}
        </div>
        <div className="header-right">
          <div className="connection-status">
            <span className="status-dot" style={{ backgroundColor: getStatusColor() }} />
            <span className="status-text">{getStatusText()}</span>
          </div>
          <div className="role-badge">
            {state.isHost ? (
              <span className="role host">👑 主持人</span>
            ) : (
              <span className="role viewer">👁 参与者</span>
            )}
          </div>
        </div>
      </header>

      <main className="app-main">
        <aside className="sidebar left-sidebar">
          <ParticipantInput
            participants={state.participants}
            groupCount={state.groupCount}
            isHost={state.isHost}
          />
        </aside>

        <section className="main-content">
          <GroupBoard
            groups={state.groups}
            participants={state.participants}
            isHost={state.isHost}
          />
        </section>

        <aside className="sidebar right-sidebar">
          <HostControls
            isHost={state.isHost}
            logs={state.logs}
            onClaimHost={handleClaimHost}
          />
        </aside>
      </main>
    </div>
  );
};

export default GroupPage;
