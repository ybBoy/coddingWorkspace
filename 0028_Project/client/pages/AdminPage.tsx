import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { eventBus, SeatData, SeatActionData } from '../core/EventBus';
import { initSocket, sendAction, getConnected } from '../core/socket';

const AdminPage: React.FC = () => {
  const [seats, setSeats] = useState<SeatData[]>([]);
  const [actions, setActions] = useState<SeatActionData[]>([]);
  const [connected, setConnected] = useState<boolean>(false);

  const handleInit = useCallback((data: { seats: SeatData[]; actions: SeatActionData[] }) => {
    setSeats(data.seats);
    setActions(data.actions);
  }, []);

  const handleUpdate = useCallback((data: { seats: SeatData[]; actions: SeatActionData[] }) => {
    setSeats(data.seats);
    setActions(data.actions);
  }, []);

  const handleConnected = useCallback(() => setConnected(true), []);
  const handleDisconnected = useCallback(() => setConnected(false), []);

  useEffect(() => {
    const unsubInit = eventBus.on('state:init', handleInit);
    const unsubUpdate = eventBus.on('state:update', handleUpdate);
    const unsubConn = eventBus.on('ws:connected', handleConnected);
    const unsubDisconn = eventBus.on('ws:disconnected', handleDisconnected);

    if (!getConnected()) {
      initSocket();
    }

    return () => {
      unsubInit();
      unsubUpdate();
      unsubConn();
      unsubDisconn();
    };
  }, [handleInit, handleUpdate, handleConnected, handleDisconnected]);

  const releasableSeats = seats.filter((s) => s.status === 'releasable');
  const occupiedSeats = seats.filter((s) => s.status === 'occupied' || s.status === 'away');

  const handleForceRelease = (seatId: number) => {
    if (window.confirm('确认强制释放该座位？')) {
      sendAction('forceRelease', { seatId });
    }
  };

  const handleReleaseAll = () => {
    if (releasableSeats.length === 0) return;
    if (window.confirm(`确认释放全部 ${releasableSeats.length} 个可释放座位？`)) {
      releasableSeats.forEach((s) => {
        sendAction('forceRelease', { seatId: s.id });
      });
    }
  };

  const getAwayMinutes = (seat: SeatData) => {
    if (seat.awaySince === 0) return 0;
    return Math.floor((Date.now() - seat.awaySince) / 60000);
  };

  return (
    <div className="admin-page">
      <header className="app-header">
        <div className="header-left">
          <h1 className="room-title">🔧 管理员面板</h1>
        </div>
        <div className="header-center">
          <span className={`conn-status ${connected ? 'online' : 'offline'}`}>
            {connected ? '● 已连接' : '○ 未连接'}
          </span>
        </div>
        <div className="header-right">
          <Link to="/" className="nav-link">← 返回座位图</Link>
        </div>
      </header>

      <div className="admin-content">
        <section className="admin-section danger">
          <div className="section-header">
            <h2>⚠ 异常座位 (可释放)</h2>
            <button
              className="btn btn-force"
              onClick={handleReleaseAll}
              disabled={releasableSeats.length === 0}
            >
              全部释放 ({releasableSeats.length})
            </button>
          </div>
          {releasableSeats.length === 0 ? (
            <p className="empty-state">暂无异常座位</p>
          ) : (
            <div className="seat-table-wrapper">
              <table className="seat-table">
                <thead>
                  <tr>
                    <th>座位号</th>
                    <th>位置</th>
                    <th>使用者</th>
                    <th>状态</th>
                    <th>暂离时长</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {releasableSeats.map((seat) => (
                    <tr key={seat.id} className="highlight-row">
                      <td>#{seat.id}</td>
                      <td>{seat.row + 1}排 {seat.col + 1}座</td>
                      <td>{seat.nickname}</td>
                      <td><span className="status-badge releasable">可释放</span></td>
                      <td>{getAwayMinutes(seat)} 分钟</td>
                      <td>
                        <button
                          className="btn btn-force"
                          onClick={() => handleForceRelease(seat.id)}
                        >
                          释放
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="admin-section">
          <h2>📋 所有占用座位</h2>
          {occupiedSeats.length === 0 ? (
            <p className="empty-state">当前没有占用的座位</p>
          ) : (
            <div className="seat-table-wrapper">
              <table className="seat-table">
                <thead>
                  <tr>
                    <th>座位号</th>
                    <th>位置</th>
                    <th>使用者</th>
                    <th>状态</th>
                    <th>暂离时长</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {occupiedSeats.map((seat) => (
                    <tr key={seat.id}>
                      <td>#{seat.id}</td>
                      <td>{seat.row + 1}排 {seat.col + 1}座</td>
                      <td>{seat.nickname}</td>
                      <td>
                        <span className={`status-badge ${seat.status}`}>
                          {seat.status === 'occupied' ? '占用中' : '暂离中'}
                        </span>
                      </td>
                      <td>{seat.status === 'away' ? `${getAwayMinutes(seat)} 分钟` : '-'}</td>
                      <td>
                        <button
                          className="btn btn-force"
                          onClick={() => handleForceRelease(seat.id)}
                        >
                          强制释放
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="admin-section">
          <h2>📊 统计信息</h2>
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-label">总座位数</div>
              <div className="stat-value">{seats.length}</div>
            </div>
            <div className="stat-card free">
              <div className="stat-label">空闲</div>
              <div className="stat-value">{seats.filter((s) => s.status === 'free').length}</div>
            </div>
            <div className="stat-card occupied">
              <div className="stat-label">占用</div>
              <div className="stat-value">{seats.filter((s) => s.status === 'occupied').length}</div>
            </div>
            <div className="stat-card away">
              <div className="stat-label">暂离</div>
              <div className="stat-value">{seats.filter((s) => s.status === 'away').length}</div>
            </div>
            <div className="stat-card releasable">
              <div className="stat-label">可释放</div>
              <div className="stat-value">{releasableSeats.length}</div>
            </div>
          </div>
        </section>

        <section className="admin-section">
          <h2>🕐 最近动态</h2>
          <div className="admin-activity">
            {actions.slice().reverse().map((action, idx) => {
              const time = new Date(action.timestamp);
              return (
                <div key={`${action.timestamp}-${idx}`} className="admin-activity-item">
                  <span className="activity-time">{time.toLocaleTimeString('zh-CN')}</span>
                  <span className="activity-nick">{action.nickname}</span>
                  <span className="activity-desc">
                    在 #{action.seatId} 座位 {action.action}
                  </span>
                </div>
              );
            })}
          </div>
        </section>
      </div>
    </div>
  );
};

export default AdminPage;
