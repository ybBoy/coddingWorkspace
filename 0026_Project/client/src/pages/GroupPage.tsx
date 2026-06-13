import React, { useState, useEffect } from 'react';
import { eventBus } from '../shared/EventBus';
import socket from '../shared/socket';
import { AppState, ConnectionStatus, AppPhase } from '../shared/types';
import LobbyPage from './LobbyPage';
import ParticipantInput from '../components/ParticipantInput';
import GroupBoard from '../components/GroupBoard';
import HostControls from '../components/HostControls';
import '../styles/GroupPage.css';

const GroupPage: React.FC = () => {
  const [phase, setPhase] = useState<AppPhase>('lobby');
  const [state, setState] = useState<AppState>({
    activityName: '活动抽签分组',
    groupCount: 4,
    roomCode: '',
    participants: [],
    groups: [],
    logs: [],
    rules: [],
    isHost: false,
  });
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('connecting');
  const [editingName, setEditingName] = useState(false);
  const [nameInput, setNameInput] = useState('');
  const [notification, setNotification] = useState<{ type: string; message: string } | null>(null);
  const [hostToken, setHostToken] = useState('');
  const [myParticipantId, setMyParticipantId] = useState<string | null>(null);
  const [selfName, setSelfName] = useState('');

  useEffect(() => {
    const handleStateUpdate = (data: AppState) => {
      setState(data);
      if (data.roomCode && phase === 'lobby') {
        setPhase('room');
      }
    };

    const handleConnectionStatus = (status: ConnectionStatus) => {
      setConnectionStatus(status);
    };

    const handleHostGranted = () => {
      showNotification('success', '已登录为主持人');
      setState((prev) => ({ ...prev, isHost: true }));
    };

    const handleRoomCreated = (data: any) => {
      setHostToken(data.hostToken || '');
      showNotification('success', `房间已创建！房间码: ${data.roomCode}`);
    };

    const handleSelfRegistered = (data: any) => {
      setMyParticipantId(data.participantId);
      showNotification('success', '报名成功！');
    };

    const handleExportData = (data: any) => {
      if (data.format === 'csv') {
        const BOM = '\uFEFF';
        const blob = new Blob([BOM + data.content], { type: 'text/csv;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${state.activityName}_分组结果.csv`;
        link.click();
        URL.revokeObjectURL(url);
        showNotification('success', 'CSV 已导出');
      }
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
    eventBus.on('room-created', handleRoomCreated);
    eventBus.on('self-registered', handleSelfRegistered);
    eventBus.on('export-data', handleExportData);
    eventBus.on('error', handleError);
    eventBus.on('success', handleSuccess);

    socket.connect();

    const urlRoom = new URLSearchParams(window.location.search).get('room');
    if (urlRoom) {
      setPhase('lobby');
    }

    return () => {
      eventBus.off('state-update', handleStateUpdate);
      eventBus.off('connection-status', handleConnectionStatus);
      eventBus.off('host-granted', handleHostGranted);
      eventBus.off('room-created', handleRoomCreated);
      eventBus.off('self-registered', handleSelfRegistered);
      eventBus.off('export-data', handleExportData);
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

  const handleSelfIdentify = () => {
    if (!selfName.trim()) return;
    const found = state.participants.find(
      (p) => p.name.toLowerCase() === selfName.trim().toLowerCase()
    );
    if (found) {
      setMyParticipantId(found.id);
      showNotification('success', `已找到你！你在${found.groupId ? '分组中' : '等待分组'}`);
    } else {
      showNotification('error', '未找到该名字，请确认后重试');
    }
  };

  const getStatusText = () => {
    switch (connectionStatus) {
      case 'connected': return '已连接';
      case 'connecting': return '连接中...';
      case 'disconnected': return '已断开';
      default: return '';
    }
  };

  const getStatusColor = () => {
    switch (connectionStatus) {
      case 'connected': return '#22c55e';
      case 'connecting': return '#f59e0b';
      case 'disconnected': return '#ef4444';
      default: return '#9ca3af';
    }
  };

  if (phase === 'lobby') {
    return (
      <div>
        {notification && (
          <div className={`notification ${notification.type}`}>
            {notification.message}
          </div>
        )}
        <LobbyPage onEnterRoom={() => setPhase('room')} />
      </div>
    );
  }

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
          <div className="room-code-display">
            房间: <strong>{state.roomCode}</strong>
          </div>
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

      {!state.isHost && !myParticipantId && (
        <div className="self-identify-bar">
          <span>找到你的分组：</span>
          <input
            type="text"
            value={selfName}
            onChange={(e) => setSelfName(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSelfIdentify()}
            placeholder="输入你的名字"
          />
          <button onClick={handleSelfIdentify}>查找</button>
        </div>
      )}

      {myParticipantId && !state.isHost && (
        <div className="my-group-bar">
          {(() => {
            const me = state.participants.find((p) => p.id === myParticipantId);
            if (!me || !me.groupId) return <span>⏳ 等待分组中...</span>;
            const myGroup = state.groups.find((g) => g.id === me.groupId);
            return <span>🎯 你在 <strong>{myGroup?.name || '未知'}</strong></span>;
          })()}
          <button className="clear-self" onClick={() => setMyParticipantId(null)}>切换身份</button>
        </div>
      )}

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
            myParticipantId={myParticipantId}
          />
        </section>

        <aside className="sidebar right-sidebar">
          <HostControls
            isHost={state.isHost}
            logs={state.logs}
            rules={state.rules}
            roomCode={state.roomCode}
            hostToken={hostToken}
            onClaimHost={handleClaimHost}
          />
        </aside>
      </main>
    </div>
  );
};

export default GroupPage;
