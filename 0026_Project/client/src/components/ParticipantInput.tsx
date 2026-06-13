import React, { useState } from 'react';
import socket from '../shared/socket';
import { Participant } from '../shared/types';

interface ParticipantInputProps {
  participants: Participant[];
  groupCount: number;
  isHost: boolean;
}

const ParticipantInput: React.FC<ParticipantInputProps> = ({
  participants,
  groupCount,
  isHost,
}) => {
  const [nameInput, setNameInput] = useState('');
  const [batchInput, setBatchInput] = useState('');
  const [groupCountInput, setGroupCountInput] = useState(groupCount.toString());
  const [showBatch, setShowBatch] = useState(false);

  const handleAddSingle = () => {
    if (!nameInput.trim() || !isHost) return;
    socket.send({ type: 'add-participant', name: nameInput.trim() });
    setNameInput('');
  };

  const handleAddBatch = () => {
    if (!batchInput.trim() || !isHost) return;
    const names = batchInput
      .split(/[\n,，、；;]/)
      .map((n) => n.trim())
      .filter((n) => n.length > 0);
    if (names.length > 0) {
      socket.send({ type: 'add-participants', names });
    }
    setBatchInput('');
    setShowBatch(false);
  };

  const handleRemove = (id: string) => {
    if (!isHost) return;
    socket.send({ type: 'remove-participant', id });
  };

  const handleClearAll = () => {
    if (!isHost) return;
    if (window.confirm('确定要清空所有参与者吗？')) {
      socket.send({ type: 'clear-participants' });
    }
  };

  const handleGroupCountChange = () => {
    if (!isHost) return;
    const count = parseInt(groupCountInput, 10);
    if (!isNaN(count) && count >= 1 && count <= 20) {
      socket.send({ type: 'set-group-count', count });
    }
  };

  return (
    <div className="participant-input">
      <h3>参与者管理</h3>

      {isHost && (
        <>
          <div className="input-section">
            <label>单个添加</label>
            <div className="input-row">
              <input
                type="text"
                value={nameInput}
                onChange={(e) => setNameInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleAddSingle()}
                placeholder="输入名字后按回车"
                disabled={!isHost}
              />
              <button onClick={handleAddSingle} disabled={!isHost}>
                添加
              </button>
            </div>
          </div>

          <div className="input-section">
            <button className="toggle-btn" onClick={() => setShowBatch(!showBatch)}>
              {showBatch ? '收起' : '批量导入'} ▾
            </button>
            {showBatch && (
              <div className="batch-input">
                <textarea
                  value={batchInput}
                  onChange={(e) => setBatchInput(e.target.value)}
                  placeholder="每行一个名字，也可以用逗号、顿号分隔"
                  rows={6}
                  disabled={!isHost}
                />
                <button onClick={handleAddBatch} disabled={!isHost}>
                  批量添加
                </button>
              </div>
            )}
          </div>

          <div className="input-section">
            <label>分组数量</label>
            <div className="input-row">
              <input
                type="number"
                min="1"
                max="20"
                value={groupCountInput}
                onChange={(e) => setGroupCountInput(e.target.value)}
                onBlur={handleGroupCountChange}
                onKeyDown={(e) => e.key === 'Enter' && handleGroupCountChange()}
                disabled={!isHost}
              />
              <span className="hint">组 (1-20)</span>
            </div>
          </div>

          {participants.length > 0 && (
            <button className="danger-btn" onClick={handleClearAll}>
              清空所有
            </button>
          )}
        </>
      )}

      <div className="participant-list">
        <div className="list-header">
          <span>参与者列表</span>
          <span className="count">{participants.length} 人</span>
        </div>
        <div className="list-content">
          {participants.length === 0 ? (
            <div className="empty">暂无参与者</div>
          ) : (
            <ul>
              {participants.map((p, index) => (
                <li key={p.id} className="participant-item">
                  <span className="index">{index + 1}.</span>
                  <span className="name">{p.name}</span>
                  {isHost && (
                    <button
                      className="remove-btn"
                      onClick={() => handleRemove(p.id)}
                      title="移除"
                    >
                      ×
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
};

export default ParticipantInput;
