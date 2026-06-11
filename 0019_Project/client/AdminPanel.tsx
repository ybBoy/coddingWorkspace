import { useState, useEffect } from 'react';
import { eventBus, EVENTS } from './EventBus';

/**
 * AdminPanel 职责：
 * - 管理员登录/登出
 * - 显示登录状态和错误提示
 * - 锁定/解锁投票
 * - 设置倒计时（1分钟/5分钟/10分钟/取消）
 * - 所有操作通过 EventBus 发出事件
 */
interface Props {
  isAdmin: boolean;
  isLocked: boolean;
  connected: boolean;
}

const TIMER_OPTIONS = [
  { label: '1 分钟', seconds: 60 },
  { label: '5 分钟', seconds: 300 },
  { label: '10 分钟', seconds: 600 },
  { label: '取消倒计时', seconds: 0 },
];

function AdminPanel({ isAdmin, isLocked, connected }: Props) {
  const [password, setPassword] = useState('');
  const [loginError, setLoginError] = useState(false);

  useEffect(() => {
    const handleOk = () => {
      setLoginError(false);
      setPassword('');
    };
    const handleFail = () => {
      setLoginError(true);
    };
    eventBus.on(EVENTS.ADMIN_LOGIN_OK, handleOk);
    eventBus.on(EVENTS.ADMIN_LOGIN_FAIL, handleFail);
    return () => {
      eventBus.off(EVENTS.ADMIN_LOGIN_OK, handleOk);
      eventBus.off(EVENTS.ADMIN_LOGIN_FAIL, handleFail);
    };
  }, []);

  const handleLogin = () => {
    if (!password.trim() || !connected) return;
    eventBus.emit(EVENTS.ADMIN_LOGIN, password.trim());
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') handleLogin();
  };

  const handleLogout = () => {
    eventBus.emit(EVENTS.ADMIN_LOGOUT);
  };

  const handleToggleLock = () => {
    if (!isAdmin || !connected) return;
    eventBus.emit(EVENTS.LOCK_VOTE, !isLocked);
  };

  const handleSetTimer = (seconds: number) => {
    if (!isAdmin || !connected) return;
    eventBus.emit(EVENTS.SET_TIMER, seconds);
  };

  if (!isAdmin) {
    return (
      <div className="admin-panel">
        <div className="panel-title" style={{ marginTop: '20px' }}>
          管理员登录
        </div>
        <div className="admin-login">
          <input
            type="password"
            placeholder="输入管理员密码（默认 admin123）"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              setLoginError(false);
            }}
            onKeyDown={handleKeyDown}
            disabled={!connected}
          />
          <button
            className="btn btn-primary"
            onClick={handleLogin}
            disabled={!connected || !password.trim()}
          >
            登录
          </button>
        </div>
        {loginError && (
          <p className="admin-error">密码错误，请重试</p>
        )}
        {!connected && (
          <p className="admin-hint">连接断开后无法登录</p>
        )}
      </div>
    );
  }

  return (
    <div className="admin-panel">
      <div className="panel-title" style={{ marginTop: '20px' }}>
        管理员面板
        <span className="admin-badge">管理员</span>
      </div>

      <div className="admin-section">
        <div className="admin-section-title">投票锁定</div>
        <div className="admin-row">
          <span className="admin-label">
            当前状态：{isLocked ? '🔒 已锁定' : '🔓 未锁定'}
          </span>
          <button
            className={`btn ${isLocked ? 'btn-secondary' : 'btn-danger'}`}
            onClick={handleToggleLock}
            disabled={!connected}
          >
            {isLocked ? '解锁投票' : '锁定投票'}
          </button>
        </div>
      </div>

      <div className="admin-section">
        <div className="admin-section-title">倒计时设置</div>
        <div className="timer-buttons">
          {TIMER_OPTIONS.map((opt) => (
            <button
              key={opt.seconds}
              className="btn btn-secondary btn-sm"
              onClick={() => handleSetTimer(opt.seconds)}
              disabled={!connected}
            >
              {opt.label}
            </button>
          ))}
        </div>
        <p className="admin-hint">
          开始倒计时后将自动解锁投票，时间结束自动锁定
        </p>
      </div>

      <div className="admin-logout">
        <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
          退出管理员
        </button>
      </div>
    </div>
  );
}

export default AdminPanel;
