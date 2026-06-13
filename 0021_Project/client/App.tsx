import React, { useState, useEffect, useRef, useCallback } from 'react';
import SessionList from './SessionList';
import BookingPanel from './BookingPanel';
import CheckInPanel from './CheckInPanel';
import LoginPanel from './LoginPanel';
import AdminPanel from './AdminPanel';
import {
  Session,
  Booking,
  WSMessage,
  ActivityItem,
  User,
  LoginForm,
  AdminSessionForm,
} from './types';
import { eventBus, EVENTS } from './EventBus';

const App: React.FC = () => {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [activities, setActivities] = useState<ActivityItem[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);
  const [connected, setConnected] = useState(false);
  const [currentUser, setCurrentUser] = useState<User | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);
  const shouldReconnectRef = useRef(true);
  const messageHandlerRef = useRef<((msg: WSMessage) => void) | null>(null);

  const selectedSession = sessions.find((s) => s.id === selectedSessionId) || null;

  const isAdmin = currentUser?.role === 'admin';

  const myBookings = currentUser
    ? bookings.filter((b) => b.employeeId === currentUser.employeeId && b.status !== 'cancelled')
    : [];

  const showToast = useCallback((message: string, type: 'success' | 'info' | 'warning' = 'info') => {
    const div = document.createElement('div');
    div.className = 'app-toast';
    div.textContent = message;
    div.style.cssText = `
      position: fixed;
      top: 24px;
      left: 50%;
      transform: translateX(-50%);
      padding: 12px 24px;
      border-radius: 8px;
      background: ${type === 'success' ? '#188038' : type === 'warning' ? '#e37400' : '#1a73e8'};
      color: white;
      font-size: 14px;
      font-weight: 500;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      z-index: 9999;
      animation: toastSlideIn 0.3s ease;
    `;
    const styleEl = document.createElement('style');
    styleEl.textContent = `
      @keyframes toastSlideIn {
        from { opacity: 0; transform: translate(-50%, -20px); }
        to { opacity: 1; transform: translate(-50%, 0); }
      }
      @keyframes toastFadeOut {
        from { opacity: 1; }
        to { opacity: 0; }
      }
    `;
    document.head.appendChild(styleEl);
    document.body.appendChild(div);
    setTimeout(() => {
      div.style.animation = 'toastFadeOut 0.3s ease forwards';
      setTimeout(() => {
        if (div.parentNode) div.parentNode.removeChild(div);
        if (styleEl.parentNode) styleEl.parentNode.removeChild(styleEl);
      }, 300);
    }, 3000);
  }, []);

  const handleMessage = useCallback(
    (msg: WSMessage) => {
      switch (msg.type) {
        case 'init':
        case 'sessions':
          if (msg.payload.sessions) {
            setSessions(msg.payload.sessions);
            eventBus.emit(EVENTS.SESSIONS_UPDATED, msg.payload.sessions);
          }
          if (msg.payload.bookings) {
            setBookings(msg.payload.bookings);
          }
          if (msg.type === 'init' && msg.payload.sessions && msg.payload.sessions.length > 0) {
            setSelectedSessionId(msg.payload.sessions[0].id);
          }
          break;

        case 'loginOk':
          if (msg.payload.user) {
            setCurrentUser(msg.payload.user);
            eventBus.emit(EVENTS.USER_UPDATED, msg.payload.user);
            showToast(
              `登录成功！欢迎，${msg.payload.user.userName}${isAdmin ? '（管理员）' : ''}`,
              'success'
            );
          }
          break;

        case 'loginFail':
          alert(msg.payload?.message || '登录失败');
          break;

        case 'sessionOk':
          if (msg.payload.sessions) {
            setSessions(msg.payload.sessions);
            eventBus.emit(EVENTS.SESSIONS_UPDATED, msg.payload.sessions);
            showToast(msg.payload.message || '操作成功', 'success');
          }
          break;

        case 'bookingOk':
          if (msg.payload.booking) {
            setBookings((prev) => {
              const index = prev.findIndex((b) => b.id === msg.payload.booking.id);
              if (index >= 0) {
                const next = [...prev];
                next[index] = msg.payload.booking;
                return next;
              }
              return [...prev, msg.payload.booking];
            });
          }
          if (msg.payload.sessions) {
            setSessions(msg.payload.sessions);
            eventBus.emit(EVENTS.SESSIONS_UPDATED, msg.payload.sessions);
          }
          break;

        case 'bookingFail':
          alert(msg.payload?.message || '预约失败');
          break;

        case 'cancelOk':
          if (msg.payload.bookingId) {
            setBookings((prev) =>
              prev.map((b) =>
                b.id === msg.payload.bookingId ? { ...b, status: 'cancelled' as const } : b
              )
            );
          }
          if (msg.payload.sessions) {
            setSessions(msg.payload.sessions);
            eventBus.emit(EVENTS.SESSIONS_UPDATED, msg.payload.sessions);
          }
          break;

        case 'checkInOk':
          if (msg.payload.bookingId) {
            setBookings((prev) =>
              prev.map((b) =>
                b.id === msg.payload.bookingId ? { ...b, status: 'checkedIn' as const } : b
              )
            );
          }
          if (msg.payload.sessions) {
            setSessions(msg.payload.sessions);
            eventBus.emit(EVENTS.SESSIONS_UPDATED, msg.payload.sessions);
          }
          break;

        case 'exportCsvOk':
          if (msg.payload.filename && msg.payload.base64Content) {
            try {
              const binaryStr = atob(msg.payload.base64Content);
              const bytes = new Uint8Array(binaryStr.length);
              for (let i = 0; i < binaryStr.length; i++) {
                bytes[i] = binaryStr.charCodeAt(i);
              }
              const blob = new Blob([bytes], { type: 'text/csv;charset=utf-8;' });
              const url = URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url;
              a.download = msg.payload.filename;
              document.body.appendChild(a);
              a.click();
              document.body.removeChild(a);
              URL.revokeObjectURL(url);
              showToast(`已导出 ${msg.payload.filename}`, 'success');
            } catch (e) {
              console.error('下载CSV失败:', e);
              alert('下载CSV失败');
            }
          }
          break;

        case 'activity':
          if (msg.payload.activity) {
            const activity: ActivityItem = msg.payload.activity;
            setActivities((prev) => {
              const next = [activity, ...prev];
              return next.slice(0, 10);
            });
            eventBus.emit(EVENTS.ACTIVITY_RECEIVED, activity);

            if (activity.promoted && currentUser) {
              const promotedMatch = activity.message.match(
                new RegExp(`${currentUser.userName}\\s*\\(${currentUser.employeeId}\\)`)
              );
              if (promotedMatch) {
                showToast('🎉 恭喜！您已从候补转为正式预约', 'success');
              }
            }
          }
          if (msg.payload.sessions) {
            setSessions(msg.payload.sessions);
            eventBus.emit(EVENTS.SESSIONS_UPDATED, msg.payload.sessions);
          }
          if (msg.payload.bookings) {
            setBookings(msg.payload.bookings);
          }
          break;

        case 'error':
          alert(msg.payload?.message || '操作失败');
          break;
      }
    },
    [isAdmin, showToast, currentUser]
  );

  useEffect(() => {
    messageHandlerRef.current = handleMessage;
  }, [handleMessage]);

  const connectWebSocket = useCallback(() => {
    shouldReconnectRef.current = true;

    if (wsRef.current) {
      try {
        wsRef.current.onclose = null as any;
        wsRef.current.close();
      } catch (e) {}
      wsRef.current = null;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws`;

    try {
      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log('WebSocket 连接成功');
        setConnected(true);
        eventBus.emit(EVENTS.CONNECTION_CHANGED, true);
        if (currentUser) {
          ws.send(
            JSON.stringify({
              type: 'login',
              payload: {
                employeeId: currentUser.employeeId,
                userName: currentUser.userName,
              },
            })
          );
        } else {
          ws.send(JSON.stringify({ type: 'init', payload: {} }));
        }
      };

      ws.onmessage = (event) => {
        try {
          const data: WSMessage = JSON.parse(event.data);
          if (messageHandlerRef.current) {
            messageHandlerRef.current(data);
          }
        } catch (e) {
          console.error('解析 WebSocket 消息失败:', e);
        }
      };

      ws.onclose = () => {
        console.log('WebSocket 连接断开');
        setConnected(false);
        eventBus.emit(EVENTS.CONNECTION_CHANGED, false);
        if (reconnectTimerRef.current) {
          clearTimeout(reconnectTimerRef.current);
          reconnectTimerRef.current = null;
        }
        if (shouldReconnectRef.current) {
          reconnectTimerRef.current = window.setTimeout(() => {
            console.log('尝试重连 WebSocket...');
            connectWebSocket();
          }, 3000);
        }
      };

      ws.onerror = (error) => {
        console.error('WebSocket 错误:', error);
      };
    } catch (e) {
      console.error('创建 WebSocket 失败:', e);
    }
  }, [currentUser]);

  const cleanupWebSocket = useCallback(() => {
    shouldReconnectRef.current = false;
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
    if (wsRef.current) {
      try {
        wsRef.current.onclose = null as any;
        wsRef.current.close();
      } catch (e) {}
      wsRef.current = null;
    }
  }, []);

  const sendMessage = useCallback((type: string, payload: any) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ type, payload }));
    } else {
      alert('未连接到服务器，请稍后重试');
    }
  }, []);

  const handleLogout = useCallback(() => {
    setCurrentUser(null);
    eventBus.emit(EVENTS.USER_UPDATED, null);
    showToast('已退出登录', 'info');
  }, [showToast]);

  useEffect(() => {
    connectWebSocket();

    const handleLoginRequest = (data: LoginForm) => {
      sendMessage('login', data);
    };
    const handleBookingRequest = (data: {
      sessionId: string;
      userName: string;
      employeeId: string;
      phone?: string;
    }) => {
      sendMessage('booking', data);
    };
    const handleCancelRequest = (data: { bookingId: string; employeeId: string }) => {
      sendMessage('cancel', data);
    };
    const handleCheckInRequest = (data: { bookingId: string }) => {
      sendMessage('checkin', data);
    };
    const handleSessionSelected = (id: string) => {
      setSelectedSessionId(id);
    };
    const handleSessionAddRequest = (data: AdminSessionForm) => {
      sendMessage('sessionAdd', data);
    };
    const handleSessionUpdateRequest = (data: AdminSessionForm) => {
      sendMessage('sessionUpdate', data);
    };
    const handleSessionCloseRequest = (data: { sessionId: string }) => {
      sendMessage('sessionClose', data);
    };
    const handleExportCsvRequest = (data: { sessionId: string }) => {
      sendMessage('exportCsv', data);
    };

    eventBus.on(EVENTS.LOGIN_REQUEST, handleLoginRequest);
    eventBus.on(EVENTS.BOOKING_REQUEST, handleBookingRequest);
    eventBus.on(EVENTS.CANCEL_REQUEST, handleCancelRequest);
    eventBus.on(EVENTS.CHECKIN_REQUEST, handleCheckInRequest);
    eventBus.on(EVENTS.SESSION_SELECTED, handleSessionSelected);
    eventBus.on(EVENTS.SESSION_ADD_REQUEST, handleSessionAddRequest);
    eventBus.on(EVENTS.SESSION_UPDATE_REQUEST, handleSessionUpdateRequest);
    eventBus.on(EVENTS.SESSION_CLOSE_REQUEST, handleSessionCloseRequest);
    eventBus.on(EVENTS.EXPORT_CSV_REQUEST, handleExportCsvRequest);

    return () => {
      eventBus.off(EVENTS.LOGIN_REQUEST, handleLoginRequest);
      eventBus.off(EVENTS.BOOKING_REQUEST, handleBookingRequest);
      eventBus.off(EVENTS.CANCEL_REQUEST, handleCancelRequest);
      eventBus.off(EVENTS.CHECKIN_REQUEST, handleCheckInRequest);
      eventBus.off(EVENTS.SESSION_SELECTED, handleSessionSelected);
      eventBus.off(EVENTS.SESSION_ADD_REQUEST, handleSessionAddRequest);
      eventBus.off(EVENTS.SESSION_UPDATE_REQUEST, handleSessionUpdateRequest);
      eventBus.off(EVENTS.SESSION_CLOSE_REQUEST, handleSessionCloseRequest);
      eventBus.off(EVENTS.EXPORT_CSV_REQUEST, handleExportCsvRequest);

      cleanupWebSocket();
    };
  }, [connectWebSocket, sendMessage, cleanupWebSocket]);

  const getActivityStyle = (type: string, promoted?: boolean) => {
    if (promoted) {
      return { color: '#188038', icon: '🎉' };
    }
    switch (type) {
      case 'booking':
        return { color: '#1a73e8', icon: '📝' };
      case 'cancel':
        return { color: '#d93025', icon: '❌' };
      case 'checkIn':
        return { color: '#188038', icon: '✅' };
      case 'waitlist':
        return { color: '#e37400', icon: '⏳' };
      case 'autoPromote':
        return { color: '#188038', icon: '⬆️' };
      default:
        return { color: '#5f6368', icon: '📢' };
    }
  };

  const todayLabel = new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'long',
  });

  if (!currentUser) {
    return <LoginPanel connected={connected} />;
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-left">
          <h1>🏋️ 实时预约签到系统</h1>
          <span className="today-label">{todayLabel}</span>
        </div>
        <div className="header-right">
          <span className={`connection-status ${connected ? 'online' : 'offline'}`}>
            <span className="status-dot"></span>
            {connected ? '已连接' : '连接断开'}
          </span>
          <span className="user-info">
            <span className="user-name">
              {currentUser.userName}（{currentUser.employeeId}）
            </span>
            {isAdmin && <span className="role-badge">管理员</span>}
            <button className="btn-logout" onClick={handleLogout}>
              退出
            </button>
          </span>
        </div>
      </header>

      {isAdmin && (
        <div className="admin-bar">
          <AdminPanel sessions={sessions} currentUser={currentUser} />
        </div>
      )}

      <main className="app-main">
        <aside className="col-left">
          <SessionList sessions={sessions} selectedId={selectedSessionId} />
        </aside>

        <section className="col-center">
          <BookingPanel
            session={selectedSession}
            myBookings={myBookings}
            currentUser={currentUser}
          />
        </section>

        {isAdmin && (
          <aside className="col-right">
            <CheckInPanel session={selectedSession} bookings={bookings} />
          </aside>
        )}
      </main>

      <footer className="app-footer">
        <div className="activity-panel">
          <h3>📢 最近动态</h3>
          <div className="activity-list">
            {activities.length === 0 && (
              <p className="no-activity">暂无动态，预约或签到后这里会显示记录</p>
            )}
            {activities.map((item) => {
              const style = getActivityStyle(item.type, item.promoted);
              return (
                <div
                  key={item.id}
                  className={`activity-item ${item.promoted ? 'activity-promoted' : ''}`}
                >
                  <span className="activity-icon">{style.icon}</span>
                  <span className="activity-time">{item.time}</span>
                  <span className="activity-text" style={{ color: style.color }}>
                    {item.message}
                    {item.promoted && <span className="promoted-badge">NEW</span>}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      </footer>

      <style>{`
        .app-container {
          display: flex;
          flex-direction: column;
          height: 100vh;
          background: #f5f7fa;
        }
        .app-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          background: white;
          padding: 12px 24px;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
          flex-shrink: 0;
        }
        .header-left {
          display: flex;
          align-items: center;
          gap: 16px;
        }
        .app-header h1 {
          font-size: 20px;
          color: #1a73e8;
          margin: 0;
        }
        .today-label {
          font-size: 13px;
          color: #5f6368;
          background: #e8f0fe;
          padding: 3px 10px;
          border-radius: 4px;
        }
        .header-right {
          display: flex;
          align-items: center;
          gap: 16px;
        }
        .connection-status {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 14px;
          font-weight: 500;
        }
        .status-dot {
          width: 10px;
          height: 10px;
          border-radius: 50%;
          display: inline-block;
        }
        .connection-status.online {
          color: #188038;
        }
        .connection-status.online .status-dot {
          background: #188038;
          box-shadow: 0 0 6px #188038;
        }
        .connection-status.offline {
          color: #d93025;
        }
        .connection-status.offline .status-dot {
          background: #d93025;
        }
        .user-info {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 14px;
        }
        .user-name {
          color: #202124;
        }
        .role-badge {
          background: #fef7e0;
          color: #e37400;
          font-size: 11px;
          padding: 2px 6px;
          border-radius: 4px;
          font-weight: 500;
        }
        .btn-logout {
          background: #f1f3f4;
          color: #5f6368;
          border: none;
          padding: 4px 10px;
          border-radius: 4px;
          font-size: 12px;
          cursor: pointer;
          transition: background 0.2s;
        }
        .btn-logout:hover {
          background: #e8eaed;
        }
        .admin-bar {
          padding: 0 16px;
          background: #f5f7fa;
          flex-shrink: 0;
        }
        .app-main {
          flex: 1;
          display: flex;
          gap: 16px;
          padding: 16px;
          min-height: 0;
        }
        .col-left {
          flex: 0 0 280px;
          min-width: 0;
        }
        .col-center {
          flex: 1;
          min-width: 0;
        }
        .col-right {
          flex: 0 0 300px;
          min-width: 0;
        }
        .app-footer {
          flex-shrink: 0;
          padding: 0 16px 16px;
        }
        .activity-panel {
          background: white;
          border-radius: 8px;
          padding: 12px 16px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
          height: 140px;
          display: flex;
          flex-direction: column;
        }
        .activity-panel h3 {
          font-size: 15px;
          color: #1a73e8;
          margin: 0 0 8px 0;
        }
        .activity-list {
          flex: 1;
          overflow-y: auto;
          display: flex;
          flex-direction: column;
          gap: 4px;
        }
        .activity-item {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 13px;
          padding: 2px 0;
          border-left: 3px solid transparent;
          padding-left: 4px;
          margin-left: -4px;
        }
        .activity-item.activity-promoted {
          background: linear-gradient(90deg, #e6f4ea, transparent);
          border-left-color: #188038;
          animation: promotedPulse 2s ease-out;
        }
        @keyframes promotedPulse {
          0% { background-color: #d2f0d8; }
          100% { background: linear-gradient(90deg, #e6f4ea, transparent); }
        }
        .activity-icon {
          font-size: 14px;
        }
        .activity-time {
          color: #80868b;
          font-size: 12px;
          min-width: 60px;
        }
        .activity-text {
          flex: 1;
          font-weight: 500;
          position: relative;
        }
        .promoted-badge {
          margin-left: 8px;
          background: #188038;
          color: white;
          font-size: 10px;
          padding: 1px 6px;
          border-radius: 3px;
          animation: badgeBlink 1.5s ease-in-out 3;
        }
        @keyframes badgeBlink {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.3; }
        }
        .no-activity {
          text-align: center;
          color: #80868b;
          font-size: 13px;
          padding: 30px 0;
          margin: 0;
        }
        @media (max-width: 1024px) {
          .col-left {
            flex: 0 0 240px;
          }
          .col-right {
            flex: 0 0 260px;
          }
        }
        @media (max-width: 768px) {
          .app-main {
            flex-direction: column;
            overflow-y: auto;
          }
          .col-left,
          .col-center,
          .col-right {
            flex: none;
            width: 100%;
          }
        }
      `}</style>
    </div>
  );
};

export default App;
