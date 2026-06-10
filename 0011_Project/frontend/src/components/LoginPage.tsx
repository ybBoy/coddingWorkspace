import React, { useState } from 'react';
import type { Role } from '../types';

interface LoginPageProps {
  onLogin: (formId: string, userName: string, role: Role) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLogin }) => {
  const [formId, setFormId] = useState('interview_001');
  const [userName, setUserName] = useState('');
  const [role, setRole] = useState<Role>('INTERVIEWER');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!userName.trim()) {
      alert('请输入姓名');
      return;
    }
    onLogin(formId.trim(), userName.trim(), role);
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <h2 className="login-title">面试评价协同系统</h2>
        <p className="login-subtitle">实时协作评分</p>
        <form className="login-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label>评价表单ID</label>
            <input
              type="text"
              value={formId}
              onChange={(e) => setFormId(e.target.value)}
              placeholder="请输入表单ID"
            />
          </div>
          <div className="form-group">
            <label>您的姓名</label>
            <input
              type="text"
              value={userName}
              onChange={(e) => setUserName(e.target.value)}
              placeholder="请输入姓名"
            />
          </div>
          <div className="form-group">
            <label>角色</label>
            <div className="role-options">
              <label className="role-option">
                <input
                  type="radio"
                  name="role"
                  value="INTERVIEWER"
                  checked={role === 'INTERVIEWER'}
                  onChange={() => setRole('INTERVIEWER')}
                />
                <span>面试官</span>
              </label>
              <label className="role-option">
                <input
                  type="radio"
                  name="role"
                  value="CANDIDATE"
                  checked={role === 'CANDIDATE'}
                  onChange={() => setRole('CANDIDATE')}
                />
                <span>候选人（只读）</span>
              </label>
            </div>
          </div>
          <button type="submit" className="login-btn">
            进入评价
          </button>
        </form>
      </div>
    </div>
  );
};
