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
  const [genderInput, setGenderInput] = useState('');
  const [deptInput, setDeptInput] = useState('');
  const [skillInput, setSkillInput] = useState('0');
  const [batchInput, setBatchInput] = useState('');
  const [groupCountInput, setGroupCountInput] = useState(groupCount.toString());
  const [showBatch, setShowBatch] = useState(false);
  const [showDetail, setShowDetail] = useState(false);

  const handleAddSingle = () => {
    if (!nameInput.trim() || !isHost) return;
    socket.send({
      type: 'add-participant',
      name: nameInput.trim(),
      gender: genderInput || undefined,
      department: deptInput || undefined,
      skill: parseInt(skillInput, 10) || 0,
    });
    setNameInput('');
    setGenderInput('');
    setDeptInput('');
    setSkillInput('0');
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
                placeholder="输入名字"
              />
              <button onClick={handleAddSingle}>添加</button>
            </div>
            <button className="toggle-btn" onClick={() => setShowDetail(!showDetail)}>
              {showDetail ? '收起详情' : '填写属性'} ▾
            </button>
            {showDetail && (
              <div className="detail-inputs">
                <div className="form-row">
                  <select value={genderInput} onChange={(e) => setGenderInput(e.target.value)}>
                    <option value="">性别</option>
                    <option value="男">男</option>
                    <option value="女">女</option>
                  </select>
                  <input
                    type="text"
                    value={deptInput}
                    onChange={(e) => setDeptInput(e.target.value)}
                    placeholder="部门"
                  />
                </div>
                <div className="form-row">
                  <label className="skill-label">能力: {skillInput}</label>
                  <input
                    type="range"
                    min="0"
                    max="10"
                    value={skillInput}
                    onChange={(e) => setSkillInput(e.target.value)}
                  />
                </div>
              </div>
            )}
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
                  rows={5}
                />
                <button onClick={handleAddBatch}>批量添加</button>
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
                  {p.gender && <span className="tag gender-tag">{p.gender}</span>}
                  {p.department && <span className="tag dept-tag">{p.department}</span>}
                  {p.selfRegistered && <span className="tag self-tag">自助</span>}
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
