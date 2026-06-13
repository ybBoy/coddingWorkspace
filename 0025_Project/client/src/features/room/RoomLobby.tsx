import React, { useState } from 'react';
import eventBus from '../../core/EventBus';
import type { RoomSummary, SocketStatus } from '../../core/types';

interface Props {
  roomList: RoomSummary[];
  userName: string;
  setUserName: (n: string) => void;
  socketStatus: SocketStatus;
}

const RoomLobby: React.FC<Props> = ({ roomList, userName, setUserName, socketStatus }) => {
  const [joinRoomId, setJoinRoomId] = useState('default');
  const [joinPasscode, setJoinPasscode] = useState('');
  const [createName, setCreateName] = useState('');
  const [createPasscode, setCreatePasscode] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  const handleJoin = (roomId: string, passcode?: string) => {
    if (!userName.trim()) {
      alert('请先输入昵称');
      return;
    }
    eventBus.emit('REQUEST_JOIN_ROOM', { roomId, passcode });
  };

  const handleCreate = () => {
    if (!userName.trim()) {
      alert('请先输入昵称');
      return;
    }
    if (!createName.trim()) {
      alert('请输入房间名');
      return;
    }
    eventBus.emit('REQUEST_CREATE_ROOM', { name: createName.trim(), passcode: createPasscode });
  };

  const handleRefresh = () => {
    eventBus.emit('REQUEST_LIST_ROOMS');
  };

  return (
    <div className="lobby">
      <header className="lobby__header">
        <div className="lobby__title">
          <span className="lobby__logo">📖</span>
          <div>
            <h1>读书会共读标注板</h1>
            <p className="lobby__subtitle">多人实时共读 · 批注 · 讨论</p>
          </div>
        </div>
        <div className="lobby__userbox">
          <input
            type="text"
            placeholder="输入你的昵称"
            value={userName}
            onChange={e => setUserName(e.target.value)}
            className="lobby__nameinput"
            maxLength={20}
          />
          <span className={`lobby__status lobby__status--${socketStatus}`}>
            {socketStatus === 'open' ? '🟢 已连接' : socketStatus === 'connecting' ? '🟡 连接中' : '🔴 已断开'}
          </span>
        </div>
      </header>

      <main className="lobby__main">
        <div className="lobby__section">
          <div className="lobby__sectionhead">
            <h2>🏠 共读房间</h2>
            <button className="btn btn--ghost" onClick={handleRefresh}>🔄 刷新</button>
          </div>

          {roomList.length === 0 ? (
            <div className="lobby__empty">暂无房间，点击右侧创建一个吧～</div>
          ) : (
            <div className="lobby__rooms">
              {roomList.map(room => (
                <div key={room.id} className="roomcard">
                  <div className="roomcard__head">
                    <h3>{room.name}</h3>
                    <span className="roomcard__badge">
                      {room.hasPasscode ? '🔒' : '🌐'}
                    </span>
                  </div>
                  <div className="roomcard__meta">
                    <span>📄 {room.articleTitle}</span>
                    <span>👤 {room.onlineCount} 人在线</span>
                    <span>🏷️ 房主: {room.ownerName}</span>
                  </div>
                  {room.hasPasscode ? (
                    <div className="roomcard__join">
                      <input
                        type="password"
                        placeholder="输入口令"
                        value={joinRoomId === room.id ? joinPasscode : ''}
                        onChange={e => { setJoinRoomId(room.id); setJoinPasscode(e.target.value); }}
                        onFocus={() => setJoinRoomId(room.id)}
                        className="roomcard__pass"
                      />
                      <button className="btn btn--primary" onClick={() => handleJoin(room.id, joinPasscode)}>
                        进入
                      </button>
                    </div>
                  ) : (
                    <button className="btn btn--primary roomcard__joinbtn" onClick={() => handleJoin(room.id)}>
                      进入房间 →
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="lobby__sidebar">
          {!showCreate ? (
            <button className="btn btn--primary lobby__createbtn" onClick={() => setShowCreate(true)}>
              ➕ 创建新房间
            </button>
          ) : (
            <div className="lobby__create">
              <h3>创建新房间</h3>
              <label>
                <span>房间名称</span>
                <input
                  type="text"
                  value={createName}
                  onChange={e => setCreateName(e.target.value)}
                  placeholder="例如：论语共读小组"
                  maxLength={30}
                />
              </label>
              <label>
                <span>访问口令（可选）</span>
                <input
                  type="text"
                  value={createPasscode}
                  onChange={e => setCreatePasscode(e.target.value)}
                  placeholder="留空表示公开"
                  maxLength={20}
                />
              </label>
              <div className="lobby__createbtns">
                <button className="btn btn--ghost" onClick={() => setShowCreate(false)}>取消</button>
                <button className="btn btn--primary" onClick={handleCreate}>创建并进入</button>
              </div>
            </div>
          )}

          <div className="lobby__tips">
            <h4>💡 使用提示</h4>
            <ul>
              <li>进入房间后可在段落旁添加批注</li>
              <li>主持人可切换共读段落、标记重点</li>
              <li>房主可设置主持人权限</li>
              <li>会后可导出 Markdown / JSON 记录</li>
              <li>支持回放模式复盘整场共读</li>
            </ul>
          </div>
        </div>
      </main>
    </div>
  );
};

export default RoomLobby;
