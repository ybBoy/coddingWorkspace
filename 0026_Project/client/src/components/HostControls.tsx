import React, { useState } from 'react';
import socket from '../shared/socket';
import { ActionLog } from '../shared/types';

interface HostControlsProps {
  isHost: boolean;
  logs: ActionLog[];
  onClaimHost: (token: string) => void;
}

const HostControls: React.FC<HostControlsProps> = ({ isHost, logs, onClaimHost }) => {
  const [tokenInput, setTokenInput] = useState('');

  const handleClaimHost = () => {
    if (tokenInput.trim()) {
      onClaimHost(tokenInput.trim().toUpperCase());
      setTokenInput('');
    }
  };

  const handleRandomGroup = () => {
    if (!isHost) return;
    socket.send({ type: 'random-group' });
  };

  const handleUndo = () => {
    if (!isHost || logs.length === 0) return;
    socket.send({ type: 'undo' });
  };

  const handleSave = () => {
    if (!isHost) return;
    socket.send({ type: 'save' });
  };

  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);
    const h = date.getHours().toString().padStart(2, '0');
    const m = date.getMinutes().toString().padStart(2, '0');
    const s = date.getSeconds().toString().padStart(2, '0');
    return `${h}:${m}:${s}`;
  };

  if (!isHost) {
    return (
      <div className="host-controls">
        <h3>主持人登录</h3>
        <div className="host-login">
          <p className="hint">输入主持人令牌以获得管理权限</p>
          <div className="input-row">
            <input
              type="text"
              value={tokenInput}
              onChange={(e) => setTokenInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleClaimHost()}
              placeholder="输入令牌"
              maxLength={6}
            />
            <button onClick={handleClaimHost}>登录</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="host-controls">
      <h3>主持人操作</h3>

      <div className="action-buttons">
        <button className="primary-btn large-btn" onClick={handleRandomGroup}>
          🎲 随机分组
        </button>

        <button
          className="secondary-btn"
          onClick={handleUndo}
          disabled={logs.length === 0}
        >
          ↩ 撤销上一步
        </button>

        <button className="secondary-btn" onClick={handleSave}>
          💾 立即保存
        </button>
      </div>

      <div className="history-section">
        <h4>操作历史</h4>
        <div className="history-list">
          {logs.length === 0 ? (
            <div className="empty-history">暂无操作记录</div>
          ) : (
            <ul>
              {[...logs].reverse().map((log, index) => (
                <li key={index} className="history-item">
                  <span className="history-time">{formatTime(log.timestamp)}</span>
                  <span className="history-desc">{log.description}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <div className="host-tip">
        <p>💡 提示：</p>
        <ul>
          <li>锁定组后重新分组，该组人员不变</li>
          <li>拖动成员可以调整分组</li>
          <li>数据每30秒自动保存一次</li>
        </ul>
      </div>
    </div>
  );
};

export default HostControls;
