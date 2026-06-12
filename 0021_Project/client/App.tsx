import React, { useState, useEffect, useRef, useCallback } from 'react';
import SessionList from './SessionList';
import BookingPanel from './BookingPanel';
import CheckInPanel from './CheckInPanel';
import { Session, Booking, WSMessage, ActivityItem } from './types';
import { eventBus, EVENTS } from './EventBus';

// App 主应用组件
// 职责：
//   1. 管理 WebSocket 连接状态（兼容 React StrictMode 开发模式下的双重 mount）
//   2. 管理全局状态：场次列表、预约列表、活动动态
//   3. 订阅 EventBus 事件，将用户操作通过 WebSocket 发送给后端
//   4. 接收后端 WebSocket 消息，更新状态并通过 EventBus 通知组件
//   5. 整体页面布局
//
// 数据流：
//   用户操作 → 子组件 emit EventBus 事件 → App 订阅 → WebSocket 发送 → Java 后端
//   Java 后端 → WebSocket 推送 → App 接收 → 更新 state → 子组件渲染
//   同时通过 EventBus 发出 SESSIONS_UPDATED / ACTIVITY_RECEIVED 事件

const App: React.FC = () => {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [activities, setActivities] = useState<ActivityItem[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);
  const [connected, setConnected] = useState(false);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);

  // 组件正常在线时，连接异常断开允许重连；真正卸载时置为 false，避免 close() 触发的 onclose 再安排重连
  const shouldReconnectRef = useRef(true);
  // 保存最新的 handleMessage，避免闭包陈旧
  const messageHandlerRef = useRef<((msg: WSMessage) => void) | null>(null);

  const selectedSession = sessions.find((s) => s.id === selectedSessionId) || null;

  // ============ 消息处理 ============
  const handleMessage = useCallback((msg: WSMessage) => {
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
              b.id === msg.payload.bookingId
                ? { ...b, status: 'cancelled' as const }
                : b
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
              b.id === msg.payload.bookingId
                ? { ...b, status: 'checkedIn' as const }
                : b
            )
          );
        }
        if (msg.payload.sessions) {
          setSessions(msg.payload.sessions);
          eventBus.emit(EVENTS.SESSIONS_UPDATED, msg.payload.sessions);
        }
        break;

      case 'activity':
        if (msg.payload.activity) {
          setActivities((prev) => {
            const next = [msg.payload.activity, ...prev];
            return next.slice(0, 10);
          });
          eventBus.emit(EVENTS.ACTIVITY_RECEIVED, msg.payload.activity);
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
  }, []);

  // 始终把最新的 handleMessage 挂到 ref 上
  useEffect(() => {
    messageHandlerRef.current = handleMessage;
  }, [handleMessage]);

  // ============ 连接逻辑 ============
  const connectWebSocket = useCallback(() => {
    // 每次建立新连接前重置"允许重连"标志（StrictMode 下第一次 cleanup 会把它置 false，第二次 mount 要恢复）
    shouldReconnectRef.current = true;

    // 如果已经存在活跃连接，先关闭再建，避免多连接
    if (wsRef.current) {
      try {
        // 先清掉 onclose，防止手动 close 触发重连逻辑
        wsRef.current.onclose = null as any;
        wsRef.current.close();
      } catch (e) {
        // ignore
      }
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
        ws.send(JSON.stringify({ type: 'init', payload: {} }));
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
        // 仅在组件仍"活着"时安排重连；真正卸载（shouldReconnectRef=false）时不再重连
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
  }, []);

  // 清理 WebSocket 与重连定时器
  // 注意：必须先置 shouldReconnectRef=false，再关闭连接，否则 onclose 里仍会安排重连
  const cleanupWebSocket = useCallback(() => {
    shouldReconnectRef.current = false;
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
    if (wsRef.current) {
      try {
        // 先清掉 onclose，防止手动 close 触发重连逻辑
        wsRef.current.onclose = null as any;
        wsRef.current.close();
      } catch (e) {
        // ignore
      }
      wsRef.current = null;
    }
  }, []);

  // ============ 发送消息 ============
  const sendMessage = useCallback((type: string, payload: any) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ type, payload }));
    } else {
      alert('未连接到服务器，请稍后重试');
    }
  }, []);

  // ============ 初始化 / 清理（兼容 StrictMode）============
  useEffect(() => {
    // 无论 StrictMode 是否启用，首次 mount 都建连接；
    // connectWebSocket 内部会先关闭旧连接再建新的，保证单连接；
    // cleanupWebSocket 会置 shouldReconnectRef=false，防止 onclose 触发额外重连。
    connectWebSocket();

    // EventBus 订阅
    const handleBookingRequest = (data: { sessionId: string; userName: string }) => {
      sendMessage('booking', data);
    };
    const handleCancelRequest = (data: { bookingId: string; userName: string }) => {
      sendMessage('cancel', data);
    };
    const handleCheckInRequest = (data: { bookingId: string }) => {
      sendMessage('checkin', data);
    };
    const handleSessionSelected = (id: string) => {
      setSelectedSessionId(id);
    };

    eventBus.on(EVENTS.BOOKING_REQUEST, handleBookingRequest);
    eventBus.on(EVENTS.CANCEL_REQUEST, handleCancelRequest);
    eventBus.on(EVENTS.CHECKIN_REQUEST, handleCheckInRequest);
    eventBus.on(EVENTS.SESSION_SELECTED, handleSessionSelected);

    return () => {
      eventBus.off(EVENTS.BOOKING_REQUEST, handleBookingRequest);
      eventBus.off(EVENTS.CANCEL_REQUEST, handleCancelRequest);
      eventBus.off(EVENTS.CHECKIN_REQUEST, handleCheckInRequest);
      eventBus.off(EVENTS.SESSION_SELECTED, handleSessionSelected);

      // 真正卸载或 StrictMode 模拟卸载时统一清理；
      // 注意内部先 shouldReconnectRef=false 再 close，避免 onclose 再安排重连
      cleanupWebSocket();
    };
    // 依赖 connectWebSocket/sendMessage 都是 useCallback，空依赖，所以这里只跑一次
  }, [connectWebSocket, sendMessage, cleanupWebSocket]);

  // ============ 样式相关 ============
  const getActivityStyle = (type: string) => {
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
        return { color: '#1a73e8', icon: '⬆️' };
      default:
        return { color: '#5f6368', icon: '📢' };
    }
  };

  // 获取今天日期用于标题
  const todayLabel = new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'long',
  });

  return (
    <div className="app-container">
      {/* 顶部标题栏 */}
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
        </div>
      </header>

      {/* 主体三栏布局 */}
      <main className="app-main">
        <aside className="col-left">
          <SessionList sessions={sessions} selectedId={selectedSessionId} />
        </aside>

        <section className="col-center">
          <BookingPanel session={selectedSession} myBookings={bookings} />
        </section>

        <aside className="col-right">
          <CheckInPanel session={selectedSession} bookings={bookings} />
        </aside>
      </main>

      {/* 底部活动动态 */}
      <footer className="app-footer">
        <div className="activity-panel">
          <h3>📢 最近动态</h3>
          <div className="activity-list">
            {activities.length === 0 && (
              <p className="no-activity">暂无动态，预约或签到后这里会显示记录</p>
            )}
            {activities.map((item) => {
              const style = getActivityStyle(item.type);
              return (
                <div key={item.id} className="activity-item">
                  <span className="activity-icon">{style.icon}</span>
                  <span className="activity-time">{item.time}</span>
                  <span className="activity-text" style={{ color: style.color }}>
                    {item.message}
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

        /* 顶部 */
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

        /* 主体三栏 */
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

        /* 底部动态 */
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
        }
        .no-activity {
          text-align: center;
          color: #80868b;
          font-size: 13px;
          padding: 30px 0;
          margin: 0;
        }

        /* 适配 iPad 横屏及以上 */
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
