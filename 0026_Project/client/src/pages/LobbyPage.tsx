import React, { useState } from 'react';
import socket from '../shared/socket';
import '../styles/LobbyPage.css';

interface LobbyPageProps {
  onEnterRoom: () => void;
}

const LobbyPage: React.FC<LobbyPageProps> = ({ onEnterRoom }) => {
  const [mode, setMode] = useState<'home' | 'create' | 'join' | 'register'>('home');
  const [activityName, setActivityName] = useState('活动抽签分组');
  const [roomCode, setRoomCode] = useState('');
  const [name, setName] = useState('');
  const [gender, setGender] = useState('');
  const [department, setDepartment] = useState('');

  const handleCreate = () => {
    if (!activityName.trim()) return;
    socket.createRoom(activityName.trim());
    onEnterRoom();
  };

  const handleJoin = () => {
    if (!roomCode.trim()) return;
    socket.joinRoom(roomCode.trim());
    onEnterRoom();
  };

  const handleRegister = () => {
    if (!roomCode.trim() || !name.trim()) return;
    socket.selfRegister(roomCode.trim(), name.trim(), gender || undefined, department || undefined);
    onEnterRoom();
  };

  const urlRoomCode = new URLSearchParams(window.location.search).get('room');

  React.useEffect(() => {
    if (urlRoomCode) {
      setRoomCode(urlRoomCode.toUpperCase());
      setMode('register');
    }
  }, [urlRoomCode]);

  return (
    <div className="lobby-page">
      <div className="lobby-container">
        <div className="lobby-header">
          <h1>🎲 活动抽签分组</h1>
          <p>实时随机分组，让活动更有趣</p>
        </div>

        {mode === 'home' && (
          <div className="lobby-actions">
            <button className="action-card" onClick={() => setMode('create')}>
              <span className="action-icon">👑</span>
              <span className="action-title">创建活动</span>
              <span className="action-desc">我是主持人，创建新活动房间</span>
            </button>
            <button className="action-card" onClick={() => setMode('join')}>
              <span className="action-icon">👁</span>
              <span className="action-title">加入活动</span>
              <span className="action-desc">输入房间码，查看分组结果</span>
            </button>
            <button className="action-card" onClick={() => setMode('register')}>
              <span className="action-icon">✋</span>
              <span className="action-title">自助报名</span>
              <span className="action-desc">输入房间码和姓名，加入活动</span>
            </button>
          </div>
        )}

        {mode === 'create' && (
          <div className="lobby-form">
            <h2>创建活动房间</h2>
            <div className="form-group">
              <label>活动名称</label>
              <input
                type="text"
                value={activityName}
                onChange={(e) => setActivityName(e.target.value)}
                placeholder="例如：新员工培训分组"
              />
            </div>
            <div className="form-actions">
              <button className="btn-secondary" onClick={() => setMode('home')}>返回</button>
              <button className="btn-primary" onClick={handleCreate}>创建房间</button>
            </div>
          </div>
        )}

        {mode === 'join' && (
          <div className="lobby-form">
            <h2>加入活动</h2>
            <div className="form-group">
              <label>房间码</label>
              <input
                type="text"
                value={roomCode}
                onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
                placeholder="输入6位房间码"
                maxLength={6}
                style={{ textTransform: 'uppercase', letterSpacing: '0.2em', textAlign: 'center' }}
              />
            </div>
            <div className="form-actions">
              <button className="btn-secondary" onClick={() => setMode('home')}>返回</button>
              <button className="btn-primary" onClick={handleJoin} disabled={roomCode.length < 4}>加入</button>
            </div>
          </div>
        )}

        {mode === 'register' && (
          <div className="lobby-form">
            <h2>自助报名</h2>
            <div className="form-group">
              <label>房间码</label>
              <input
                type="text"
                value={roomCode}
                onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
                placeholder="输入6位房间码"
                maxLength={6}
                style={{ textTransform: 'uppercase', letterSpacing: '0.2em', textAlign: 'center' }}
              />
            </div>
            <div className="form-group">
              <label>你的姓名</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="请输入你的名字"
              />
            </div>
            <div className="form-row">
              <div className="form-group half">
                <label>性别 (选填)</label>
                <select value={gender} onChange={(e) => setGender(e.target.value)}>
                  <option value="">不填</option>
                  <option value="男">男</option>
                  <option value="女">女</option>
                </select>
              </div>
              <div className="form-group half">
                <label>部门 (选填)</label>
                <input
                  type="text"
                  value={department}
                  onChange={(e) => setDepartment(e.target.value)}
                  placeholder="如：技术部"
                />
              </div>
            </div>
            <div className="form-actions">
              <button className="btn-secondary" onClick={() => setMode('home')}>返回</button>
              <button
                className="btn-primary"
                onClick={handleRegister}
                disabled={!roomCode.trim() || !name.trim()}
              >
                报名加入
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default LobbyPage;
