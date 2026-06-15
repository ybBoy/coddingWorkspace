import React, { useState, useEffect, useRef, useCallback } from 'react';
import { eventBus } from '../shared/EventBus';
import socket from '../shared/socket';
import { AppState, ConnectionStatus, Group, Participant } from '../shared/types';
import './BigScreenPage.css';

interface BigScreenPageProps {
  onExit: () => void;
}

const BigScreenPage: React.FC<BigScreenPageProps> = ({ onExit }) => {
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
  const [showCountdown, setShowCountdown] = useState(false);
  const [countdownSeconds, setCountdownSeconds] = useState(0);
  const [countdownInput, setCountdownInput] = useState(5);
  const [isCountdownRunning, setIsCountdownRunning] = useState(false);
  const [hintIndex, setHintIndex] = useState(0);
  const countdownTimerRef = useRef<number | null>(null);
  const hintTimerRef = useRef<number | null>(null);

  const hints = [
    '欢迎来到活动分组大屏！',
    '主持人可以在后台进行分组操作',
    '请各位参与者耐心等待分组结果',
    '分组完成后请找到自己所在的小组',
    '祝大家活动愉快，合作顺利！',
    '如有疑问，请咨询现场工作人员',
  ];

  const groupColors = [
    'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    'linear-gradient(135deg, #30cfd0 0%, #330867 100%)',
    'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
    'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  ];

  useEffect(() => {
    const handleStateUpdate = (data: AppState) => {
      setState(data);
    };

    const handleConnectionStatus = (status: ConnectionStatus) => {
      setConnectionStatus(status);
    };

    eventBus.on('state-update', handleStateUpdate);
    eventBus.on('connection-status', handleConnectionStatus);

    if (socket.getStatus() === 'disconnected') {
      socket.connect();
    }

    return () => {
      eventBus.off('state-update', handleStateUpdate);
      eventBus.off('connection-status', handleConnectionStatus);
      if (countdownTimerRef.current) {
        window.clearInterval(countdownTimerRef.current);
      }
      if (hintTimerRef.current) {
        window.clearInterval(hintTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    hintTimerRef.current = window.setInterval(() => {
      setHintIndex((prev) => (prev + 1) % hints.length);
    }, 4000);

    return () => {
      if (hintTimerRef.current) {
        window.clearInterval(hintTimerRef.current);
      }
    };
  }, [hints.length]);

  const startCountdown = useCallback(() => {
    if (countdownInput <= 0) return;
    setCountdownSeconds(countdownInput * 60);
    setIsCountdownRunning(true);
    countdownTimerRef.current = window.setInterval(() => {
      setCountdownSeconds((prev) => {
        if (prev <= 1) {
          if (countdownTimerRef.current) {
            window.clearInterval(countdownTimerRef.current);
          }
          setIsCountdownRunning(false);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, [countdownInput]);

  const stopCountdown = useCallback(() => {
    if (countdownTimerRef.current) {
      window.clearInterval(countdownTimerRef.current);
    }
    setIsCountdownRunning(false);
    setCountdownSeconds(0);
  }, []);

  const formatCountdown = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
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

  const getParticipantsForGroup = (group: Group): Participant[] => {
    return group.participantIds
      .map((id) => state.participants.find((p) => p.id === id))
      .filter((p): p is Participant => !!p);
  };

  const getGridCols = () => {
    const count = state.groups.length;
    if (count <= 2) return 2;
    if (count <= 4) return 2;
    if (count <= 6) return 3;
    if (count <= 9) return 3;
    return 4;
  };

  return (
    <div className="big-screen-container">
      <div className="big-screen-bg">
        <div className="bg-glow glow-1" />
        <div className="bg-glow glow-2" />
        <div className="bg-glow glow-3" />
      </div>

      <header className="big-screen-header">
        <div className="header-title-section">
          <h1 className="big-activity-title">{state.activityName}</h1>
          <div className="big-room-code">
            <span className="room-code-label">房间码</span>
            <span className="room-code-value">{state.roomCode || '------'}</span>
          </div>
        </div>

        <div className="header-right-section">
          <div className="big-connection-status">
            <span
              className="status-indicator"
              style={{ backgroundColor: getStatusColor() }}
            />
            <span className="status-label">{getStatusText()}</span>
          </div>

          <div className="header-stats">
            <div className="stat-item">
              <span className="stat-value">{state.participants.length}</span>
              <span className="stat-label">参与者</span>
            </div>
            <div className="stat-divider" />
            <div className="stat-item">
              <span className="stat-value">{state.groups.length}</span>
              <span className="stat-label">小组</span>
            </div>
          </div>

          <button className="exit-big-screen-btn" onClick={onExit}>
            <span className="exit-icon">✕</span>
            退出大屏
          </button>
        </div>
      </header>

      <main className="big-screen-main">
        <div
          className="big-group-board"
          style={{
            gridTemplateColumns: `repeat(${getGridCols()}, 1fr)`,
          }}
        >
          {state.groups.map((group, index) => {
            const members = getParticipantsForGroup(group);
            const color = groupColors[index % groupColors.length];
            return (
              <div
                key={group.id}
                className={`big-group-card ${group.locked ? 'locked' : ''}`}
              >
                <div
                  className="big-group-header"
                  style={{ background: color }}
                >
                  <h2 className="big-group-name">{group.name}</h2>
                  <div className="big-group-meta">
                    <span className="big-group-count">{members.length}人</span>
                    {group.locked && <span className="lock-icon">🔒</span>}
                  </div>
                </div>
                <div className="big-group-members">
                  {members.length === 0 ? (
                    <div className="big-empty-members">等待成员加入...</div>
                  ) : (
                    <ul>
                      {members.map((member, idx) => (
                        <li key={member.id} className="big-member-item">
                          <span
                            className="big-member-idx"
                            style={{ background: color }}
                          >
                            {idx + 1}
                          </span>
                          <span className="big-member-name">{member.name}</span>
                          {member.department && (
                            <span className="big-member-dept">
                              {member.department}
                            </span>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </main>

      <footer className="big-screen-footer">
        <div className="hint-ticker">
          <span className="hint-icon">💡</span>
          <span className="hint-text" key={hintIndex}>
            {hints[hintIndex]}
          </span>
        </div>

        <div className="footer-right-section">
          {!showCountdown ? (
            <button
              className="countdown-toggle-btn"
              onClick={() => setShowCountdown(true)}
            >
              ⏱ 开启倒计时
            </button>
          ) : (
            <div className="countdown-panel">
              {!isCountdownRunning && countdownSeconds === 0 && (
                <div className="countdown-setup">
                  <input
                    type="number"
                    min="1"
                    max="999"
                    value={countdownInput}
                    onChange={(e) =>
                      setCountdownInput(
                        Math.max(1, Math.min(999, parseInt(e.target.value) || 1))
                      )
                    }
                    className="countdown-input"
                  />
                  <span className="countdown-unit">分钟</span>
                  <button
                    className="countdown-start-btn"
                    onClick={startCountdown}
                  >
                    开始
                  </button>
                </div>
              )}

              {(isCountdownRunning || countdownSeconds > 0) && (
                <div className="countdown-display">
                  <span
                    className={`countdown-time ${
                      countdownSeconds <= 60 ? 'warning' : ''
                    }`}
                  >
                    {formatCountdown(countdownSeconds)}
                  </span>
                  <button
                    className="countdown-stop-btn"
                    onClick={stopCountdown}
                  >
                    停止
                  </button>
                </div>
              )}

              {!isCountdownRunning && countdownSeconds === 0 && (
                <button
                  className="countdown-close-btn"
                  onClick={() => setShowCountdown(false)}
                >
                  关闭
                </button>
              )}
            </div>
          )}
        </div>
      </footer>
    </div>
  );
};

export default BigScreenPage;
