import React, { useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { LoginForm } from './types';

// LoginPanel 登录面板
// 职责：提供用户登录界面，输入工号+姓名，点击登录后通过 EventBus 发送 LOGIN_REQUEST
// 管理员账号：工号为 "admin" 或 "A001"/"A002" 的用户自动获得管理员权限
interface LoginPanelProps {
  connected: boolean;
}

const LoginPanel: React.FC<LoginPanelProps> = ({ connected }) => {
  const [employeeId, setEmployeeId] = useState('');
  const [userName, setUserName] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!employeeId.trim() || !userName.trim()) {
      alert('请输入工号和姓名');
      return;
    }
    setIsSubmitting(true);
    const data: LoginForm = {
      employeeId: employeeId.trim(),
      userName: userName.trim(),
    };
    eventBus.emit(EVENTS.LOGIN_REQUEST, data);
    setTimeout(() => setIsSubmitting(false), 500);
  };

  return (
    <div className="login-overlay">
      <div className="login-card">
        <div className="login-header">
          <div className="login-icon">🏋️</div>
          <h2>实时预约签到系统</h2>
          <p className="login-subtitle">请输入您的工号和姓名登录</p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label>工号（必填）</label>
            <input
              type="text"
              value={employeeId}
              onChange={(e) => setEmployeeId(e.target.value)}
              placeholder="如：A001 或 admin"
              autoFocus
              disabled={isSubmitting}
            />
          </div>

          <div className="form-group">
            <label>姓名（必填）</label>
            <input
              type="text"
              value={userName}
              onChange={(e) => setUserName(e.target.value)}
              placeholder="请输入您的姓名"
              disabled={isSubmitting}
            />
          </div>

          <div className="form-tip">
            💡 管理员工号：admin / A001 / A002
          </div>

          <div className={`connection-row ${connected ? 'online' : 'offline'}`}>
            <span className="status-dot"></span>
            {connected ? '服务器已连接' : '正在连接服务器...'}
          </div>

          <button type="submit" className="login-btn" disabled={isSubmitting || !connected}>
            {isSubmitting ? '登录中...' : '登 录'}
          </button>
        </form>
      </div>

      <style>{`
        .login-overlay {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 9999;
        }

        .login-card {
          background: white;
          border-radius: 16px;
          padding: 40px 32px;
          width: 100%;
          max-width: 400px;
          box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        }

        .login-header {
          text-align: center;
          margin-bottom: 24px;
        }

        .login-icon {
          font-size: 48px;
          margin-bottom: 8px;
        }

        .login-header h2 {
          margin: 0 0 4px 0;
          color: #1a73e8;
          font-size: 22px;
        }

        .login-subtitle {
          margin: 0;
          color: #5f6368;
          font-size: 13px;
        }

        .login-form {
          display: flex;
          flex-direction: column;
          gap: 16px;
        }

        .form-group {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .form-group label {
          font-size: 13px;
          font-weight: 500;
          color: #3c4043;
        }

        .form-group input {
          padding: 12px 14px;
          border: 1px solid #dadce0;
          border-radius: 8px;
          font-size: 14px;
          outline: none;
          transition: all 0.2s;
        }

        .form-group input:focus {
          border-color: #1a73e8;
          box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.1);
        }

        .form-group input:disabled {
          background: #f5f5f5;
        }

        .form-tip {
          font-size: 12px;
          color: #5f6368;
          background: #e8f0fe;
          padding: 8px 12px;
          border-radius: 6px;
          text-align: center;
        }

        .connection-row {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 12px;
          padding: 6px 12px;
          border-radius: 6px;
          justify-content: center;
        }
        .connection-row.online {
          color: #188038;
          background: #e6f4ea;
        }
        .connection-row.offline {
          color: #d93025;
          background: #fce8e6;
        }
        .connection-row .status-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          display: inline-block;
        }
        .connection-row.online .status-dot {
          background: #188038;
        }
        .connection-row.offline .status-dot {
          background: #d93025;
          animation: blink 1s infinite;
        }
        @keyframes blink {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.3; }
        }

        .login-btn {
          padding: 12px;
          background: #1a73e8;
          color: white;
          border: none;
          border-radius: 8px;
          font-size: 15px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s;
        }

        .login-btn:hover:not(:disabled) {
          background: #1557b0;
        }

        .login-btn:disabled {
          background: #9aa0a6;
          cursor: not-allowed;
        }
      `}</style>
    </div>
  );
};

export default LoginPanel;
