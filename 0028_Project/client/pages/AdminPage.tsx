import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  eventBus,
  SeatData,
  SeatActionData,
  StatsData,
  ZONE_LABELS,
  ZoneType,
} from '../core/EventBus';
import { initSocket } from '../core/socket';
import Toast from '../components/Toast';
import BroadcastBanner from '../components/BroadcastBanner';

const AdminPage: React.FC = () => {
  const [seats, setSeats] = useState<SeatData[]>([]);
  const [actions, setActions] = useState<SeatActionData[]>([]);
  const [connected, setConnected] = useState<boolean>(false);
  const [isAdmin, setIsAdmin] = useState<boolean>(() => {
    return localStorage.getItem('studyroom_admin') === '1';
  });
  const [password, setPassword] = useState('');
  const [stats, setStats] = useState<StatsData | null>(null);
  const [broadcastText, setBroadcastText] = useState('');
  const [activeZone, setActiveZone] = useState<ZoneType | 'all'>('all');

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

  const handleLoginResult = useCallback((data: { success: boolean }) => {
    if (data.success) {
      setIsAdmin(true);
      localStorage.setItem('studyroom_admin', '1');
      eventBus.emit('toast:show', { message: '管理员登录成功', type: 'success' });
      eventBus.emit('admin:requestStats');
    } else {
      setIsAdmin(false);
      localStorage.removeItem('studyroom_admin');
      eventBus.emit('toast:show', { message: '口令错误', type: 'error' });
    }
  }, []);

  const handleStats = useCallback((data: StatsData) => {
    setStats(data);
  }, []);

  const handleExport = useCallback((data: SeatActionData[]) => {
    const csvContent = [
      ['时间', '座位号', '操作', '昵称'].join(','),
      ...data.map((a) => [
        new Date(a.timestamp).toLocaleString('zh-CN'),
        a.seatId,
        a.action,
        a.nickname || '',
      ].join(',')),
    ].join('\n');

    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `座位操作记录_${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    eventBus.emit('toast:show', { message: '导出成功', type: 'success' });
  }, []);

  useEffect(() => {
    const unsubInit = eventBus.on('state:init', handleInit);
    const unsubUpdate = eventBus.on('state:update', handleUpdate);
    const unsubConn = eventBus.on('ws:connected', handleConnected);
    const unsubDisconn = eventBus.on('ws:disconnected', handleDisconnected);
    const unsubLogin = eventBus.on('admin:loginResult', handleLoginResult);
    const unsubStats = eventBus.on('admin:stats', handleStats);
    const unsubExport = eventBus.on('admin:export', handleExport);

    initSocket();

    if (isAdmin) {
      setTimeout(() => eventBus.emit('admin:requestStats'), 500);
    }

    return () => {
      unsubInit();
      unsubUpdate();
      unsubConn();
      unsubDisconn();
      unsubLogin();
      unsubStats();
      unsubExport();
    };
  }, [handleInit, handleUpdate, handleConnected, handleDisconnected, handleLoginResult, handleStats, handleExport, isAdmin]);

  useEffect(() => {
    if (isAdmin) {
      const interval = setInterval(() => {
        eventBus.emit('admin:requestStats');
      }, 30000);
      return () => clearInterval(interval);
    }
  }, [isAdmin]);

  const handleLogin = () => {
    if (!password.trim()) {
      eventBus.emit('toast:show', { message: '请输入管理口令', type: 'warning' });
      return;
    }
    eventBus.emit('admin:login', { token: password.trim() });
  };

  const handleLogout = () => {
    setIsAdmin(false);
    localStorage.removeItem('studyroom_admin');
    eventBus.emit('toast:show', { message: '已退出管理员', type: 'info' });
  };

  const handleForceRelease = (seatId: number) => {
    eventBus.emit('seat:forceRelease', { seatId, isAdmin: true });
    eventBus.emit('toast:show', { message: `已释放 #${seatId} 座位`, type: 'success' });
  };

  const handleReleaseAll = () => {
    const releasableSeats = seats.filter((s) => s.status === 'releasable');
    if (releasableSeats.length === 0) return;
    releasableSeats.forEach((s) => {
      eventBus.emit('seat:forceRelease', { seatId: s.id, isAdmin: true });
    });
    eventBus.emit('toast:show', { message: `已释放 ${releasableSeats.length} 个异常座位`, type: 'success' });
  };

  const handleBroadcast = () => {
    if (!broadcastText.trim()) {
      eventBus.emit('toast:show', { message: '请输入广播内容', type: 'warning' });
      return;
    }
    eventBus.emit('admin:broadcast', { message: broadcastText.trim() });
    eventBus.emit('toast:show', { message: '广播已发送', type: 'success' });
  };

  const handleClearBroadcast = () => {
    eventBus.emit('admin:broadcast', { message: '' });
    setBroadcastText('');
    eventBus.emit('toast:show', { message: '广播已清除', type: 'info' });
  };

  const releasableSeats = seats.filter((s) => s.status === 'releasable');
  const occupiedSeats = seats.filter(
    (s) => s.status === 'occupied' || s.status === 'away'
  );

  if (!isAdmin) {
    return (
      <div className="admin-login-page">
        <Toast />
        <div className="admin-login-box">
          <h2>🔧 管理员登录</h2>
          <p className="login-desc">请输入管理口令以进入管理面板</p>
          <input
            type="password"
            className="admin-password-input"
            placeholder="请输入管理口令"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
          />
          <button className="btn btn-admin-login" onClick={handleLogin}>
            登录
          </button>
          <Link to="/" className="back-link">← 返回座位图</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <Toast />
      <BroadcastBanner />
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
          <button className="btn-logout" onClick={handleLogout}>退出管理</button>
          <Link to="/" className="nav-link">← 返回座位图</Link>
        </div>
      </header>

      <div className="admin-content">
        <section className="admin-section broadcast-section">
          <h2>📢 管理员广播</h2>
          <div className="broadcast-controls">
            <input
              type="text"
              className="broadcast-input"
              placeholder="输入广播内容（如：闭馆通知、设备维护等）"
              value={broadcastText}
              onChange={(e) => setBroadcastText(e.target.value)}
              maxLength={100}
            />
            <button className="btn btn-broadcast" onClick={handleBroadcast}>
              发送广播
            </button>
            <button className="btn btn-clear" onClick={handleClearBroadcast}>
              清除
            </button>
          </div>
        </section>

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
                    <th>区域</th>
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
                      <td>{ZONE_LABELS[seat.zone as ZoneType] || '普通区'}</td>
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
          <div className="section-header">
            <h2>📋 所有占用座位</h2>
            <div className="zone-filter-small">
              <select
                value={activeZone}
                onChange={(e) => setActiveZone(e.target.value as any)}
              >
                <option value="all">全部区域</option>
                <option value="window">靠窗区</option>
                <option value="computer">电脑区</option>
                <option value="quiet">安静区</option>
                <option value="standard">普通区</option>
              </select>
            </div>
          </div>
          {occupiedSeats.length === 0 ? (
            <p className="empty-state">当前没有占用的座位</p>
          ) : (
            <div className="seat-table-wrapper">
              <table className="seat-table">
                <thead>
                  <tr>
                    <th>座位号</th>
                    <th>位置</th>
                    <th>区域</th>
                    <th>使用者</th>
                    <th>状态</th>
                    <th>暂离时长</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {occupiedSeats
                    .filter((s) => activeZone === 'all' || s.zone === activeZone)
                    .map((seat) => (
                    <tr key={seat.id}>
                      <td>#{seat.id}</td>
                      <td>{seat.row + 1}排 {seat.col + 1}座</td>
                      <td>{ZONE_LABELS[seat.zone as ZoneType] || '普通区'}</td>
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

        {stats && (
          <section className="admin-section">
            <div className="section-header">
              <h2>📊 统计信息</h2>
              <button
                className="btn btn-export"
                onClick={() => eventBus.emit('admin:requestExport')}
              >
                导出记录
              </button>
            </div>
            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-label">总座位数</div>
                <div className="stat-value">{stats.total}</div>
              </div>
              <div className="stat-card free">
                <div className="stat-label">空闲</div>
                <div className="stat-value">{stats.free}</div>
              </div>
              <div className="stat-card occupied">
                <div className="stat-label">占用</div>
                <div className="stat-value">{stats.occupied}</div>
              </div>
              <div className="stat-card away">
                <div className="stat-label">暂离</div>
                <div className="stat-value">{stats.away}</div>
              </div>
              <div className="stat-card releasable">
                <div className="stat-label">可释放</div>
                <div className="stat-value">{stats.releasable}</div>
              </div>
              <div className="stat-card rate">
                <div className="stat-label">上座率</div>
                <div className="stat-value">{stats.occupancyRate}%</div>
              </div>
              <div className="stat-card">
                <div className="stat-label">今日独立用户</div>
                <div className="stat-value">{stats.todayUniqueUsers}</div>
              </div>
              <div className="stat-card danger">
                <div className="stat-label">今日强制释放</div>
                <div className="stat-value">{stats.todayForceReleases}</div>
              </div>
            </div>

            <h3 className="stats-subtitle">各区域情况</h3>
            <div className="zone-stats-grid">
              {Object.entries(stats.zoneStats).map(([zone, counts]) => (
                <div key={zone} className={`zone-stat-card zone-${zone}`}>
                  <div className="zone-stat-name">
                    {ZONE_LABELS[zone as ZoneType] || zone}
                  </div>
                  <div className="zone-stat-row">
                    <span>空闲 {counts[0]}</span>
                    <span>占用 {counts[1]}</span>
                    <span>暂离 {counts[2]}</span>
                    <span>可释放 {counts[3]}</span>
                  </div>
                </div>
              ))}
            </div>

            <h3 className="stats-subtitle">今日入座时段分布</h3>
            <div className="hourly-chart">
              {stats.hourlyDistribution.map((count, hour) => (
                <div key={hour} className="hourly-bar-wrapper">
                  <div className="hourly-bar" style={{ height: `${Math.max(count * 4, 2)}px` }} />
                  <span className="hourly-label">{hour}时</span>
                </div>
              ))}
            </div>
          </section>
        )}

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
                    {action.seatId >= 0 ? `在 #${action.seatId} 座位` : ''} {action.action}
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

function getAwayMinutes(seat: SeatData) {
  if (seat.awaySince === 0) return 0;
  return Math.floor((Date.now() - seat.awaySince) / 60000);
}

export default AdminPage;
