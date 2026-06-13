import React, { useState, useEffect, useMemo } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import socket from '../shared/socket';
import { eventBus } from '../shared/EventBus';
import { ActionLog, GroupRule, Participant, Group, ActivityTemplate } from '../shared/types';

interface AssignRow {
  id: string;
  name: string;
  groupId: string;
}

interface TogetherRow {
  id: string;
  names: string;
}

interface AssignRule {
  id: string;
  name: string;
  groupId: string;
}

interface TogetherRule {
  id: string;
  participantIds: string[];
}

interface HostControlsProps {
  isHost: boolean;
  logs: ActionLog[];
  rules: GroupRule[];
  roomCode: string;
  hostToken: string;
  onClaimHost: (token: string) => void;
  participants: Participant[];
  groups: Group[];
  requireApproval: boolean;
  groupMinSize?: number;
  groupMaxSize?: number;
  templates: ActivityTemplate[];
  onEnterBigScreen?: () => void;
}

const genId = () => Math.random().toString(36).slice(2, 10);

const HostControls: React.FC<HostControlsProps> = ({
  isHost, logs, rules, roomCode, hostToken, onClaimHost,
  participants, groups, requireApproval, groupMinSize, groupMaxSize, templates,
  onEnterBigScreen,
}) => {
  const [tokenInput, setTokenInput] = useState('');
  const [showRules, setShowRules] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [showExport, setShowExport] = useState(false);
  const [showQr, setShowQr] = useState(false);
  const [showApproval, setShowApproval] = useState(false);
  const [showTemplates, setShowTemplates] = useState(false);
  const [showRejected, setShowRejected] = useState(false);

  const [genderBalance, setGenderBalance] = useState(false);
  const [deptSpread, setDeptSpread] = useState(false);
  const [skillBalance, setSkillBalance] = useState(false);
  const [separateInput, setSeparateInput] = useState('');
  const [tagBalance, setTagBalance] = useState(false);

  const [assignSelectName, setAssignSelectName] = useState('');
  const [assignSelectGroupId, setAssignSelectGroupId] = useState('');
  const [assignRulesList, setAssignRulesList] = useState<AssignRule[]>([]);

  const [togetherSelectedIds, setTogetherSelectedIds] = useState<string[]>([]);
  const [togetherRulesList, setTogetherRulesList] = useState<TogetherRule[]>([]);

  const [minSizeInput, setMinSizeInput] = useState<string>('');
  const [maxSizeInput, setMaxSizeInput] = useState<string>('');

  const [showSaveTemplate, setShowSaveTemplate] = useState(false);
  const [templateNameInput, setTemplateNameInput] = useState('');
  const [localTemplates, setLocalTemplates] = useState<ActivityTemplate[]>([]);
  const [confirmApplyTemplateId, setConfirmApplyTemplateId] = useState<string | null>(null);

  useEffect(() => {
    setLocalTemplates(templates);
  }, [templates]);

  useEffect(() => {
    const handleTemplateCreated = () => {
      socket.listTemplates();
    };
    const handleTemplatesList = (data: any) => {
      if (data && data.templates) {
        setLocalTemplates(data.templates);
      }
    };
    eventBus.on('template-created', handleTemplateCreated);
    eventBus.on('templates-list', handleTemplatesList);
    return () => {
      eventBus.off('template-created', handleTemplateCreated);
      eventBus.off('templates-list', handleTemplatesList);
    };
  }, []);

  useEffect(() => {
    setGenderBalance(rules.some((r) => r.type === 'gender-balance'));
    setDeptSpread(rules.some((r) => r.type === 'dept-spread'));
    setSkillBalance(rules.some((r) => r.type === 'skill-balance'));
    setTagBalance(rules.some((r) => r.type === 'tag-balance'));
    const sepRule = rules.find((r) => r.type === 'separate');
    setSeparateInput(sepRule ? sepRule.value : '');

    const assignRules = rules.filter((r) => r.type === 'assign');
    if (assignRules.length > 0) {
      setAssignRulesList(assignRules.map((r) => {
        const [name, groupId] = r.value.split(',');
        return { id: genId(), name: name || '', groupId: groupId || '' };
      }));
    } else {
      setAssignRulesList([]);
    }

    const togetherRules = rules.filter((r) => r.type === 'together');
    if (togetherRules.length > 0) {
      setTogetherRulesList(togetherRules.map((r) => {
        const names = r.value.split(',').map((n) => n.trim()).filter(Boolean);
        const ids = names.map((name) => {
          const p = participants.find((pp) => pp.name === name);
          return p ? p.id : '';
        }).filter(Boolean);
        return { id: genId(), participantIds: ids };
      }));
    } else {
      setTogetherRulesList([]);
    }

    setMinSizeInput(groupMinSize !== undefined ? String(groupMinSize) : '');
    setMaxSizeInput(groupMaxSize !== undefined ? String(groupMaxSize) : '');
  }, [rules, groupMinSize, groupMaxSize, participants]);

  const pendingParticipants = useMemo(() => {
    return participants.filter((p) => p.registerStatus === 'pending');
  }, [participants]);

  const rejectedParticipants = useMemo(() => {
    return participants.filter((p) => p.registerStatus === 'rejected');
  }, [participants]);

  const availableParticipants = useMemo(() => {
    return participants.filter((p) => p.registerStatus !== 'rejected');
  }, [participants]);

  const getDuplicates = (p: Participant) => {
    return participants.filter(
      (other) =>
        other.id !== p.id &&
        other.name === p.name &&
        other.gender === p.gender &&
        other.department === p.department
    );
  };

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

  const handleRestoreVersion = (index: number) => {
    if (!isHost) return;
    socket.send({ type: 'restore-version', versionIndex: index });
  };

  const handleSave = () => {
    if (!isHost) return;
    socket.send({ type: 'save' });
  };

  const handleAddAssignRule = () => {
    if (!assignSelectName.trim() || !assignSelectGroupId) return;
    const newRule: AssignRule = {
      id: genId(),
      name: assignSelectName.trim(),
      groupId: assignSelectGroupId,
    };
    setAssignRulesList([...assignRulesList, newRule]);
    setAssignSelectName('');
    setAssignSelectGroupId('');
  };

  const handleRemoveAssignRule = (id: string) => {
    setAssignRulesList(assignRulesList.filter((r) => r.id !== id));
  };

  const handleAddTogetherRule = () => {
    if (togetherSelectedIds.length < 2) return;
    const newRule: TogetherRule = {
      id: genId(),
      participantIds: [...togetherSelectedIds],
    };
    setTogetherRulesList([...togetherRulesList, newRule]);
    setTogetherSelectedIds([]);
  };

  const handleRemoveTogetherRule = (id: string) => {
    setTogetherRulesList(togetherRulesList.filter((r) => r.id !== id));
  };

  const handleToggleTogetherSelect = (id: string) => {
    if (togetherSelectedIds.includes(id)) {
      setTogetherSelectedIds(togetherSelectedIds.filter((i) => i !== id));
    } else {
      setTogetherSelectedIds([...togetherSelectedIds, id]);
    }
  };

  const getParticipantNames = (ids: string[]) => {
    return ids.map((id) => participants.find((p) => p.id === id)?.name || '').filter(Boolean).join(', ');
  };

  const handleApplyRules = () => {
    if (!isHost) return;
    const newRules: GroupRule[] = [];
    if (genderBalance) newRules.push({ type: 'gender-balance', value: 'true' });
    if (deptSpread) newRules.push({ type: 'dept-spread', value: 'true' });
    if (skillBalance) newRules.push({ type: 'skill-balance', value: 'true' });
    if (tagBalance) newRules.push({ type: 'tag-balance', value: '1' });
    if (separateInput.trim()) {
      separateInput.split(/[;\n]/).filter((s) => s.trim()).forEach((pair) => {
        newRules.push({ type: 'separate', value: pair.trim() });
      });
    }
    assignRulesList.forEach((rule) => {
      newRules.push({ type: 'assign', value: `${rule.name},${rule.groupId}` });
    });
    togetherRulesList.forEach((rule) => {
      const names = getParticipantNames(rule.participantIds);
      if (names) {
        newRules.push({ type: 'together', value: names });
      }
    });
    socket.send({ type: 'set-rules', rules: newRules });

    const minSize = parseInt(minSizeInput) || 0;
    const maxSize = parseInt(maxSizeInput) || 0;
    socket.setGroupSizeLimits(minSize, maxSize);

    setShowRules(false);
  };

  const handleExportCsv = () => {
    if (!isHost) return;
    socket.send({ type: 'export-csv' });
  };

  const handleExportImage = () => {
    if (!isHost) return;
    const board = document.querySelector('.group-board');
    if (!board) return;

    const canvas = document.createElement('canvas');
    const groupEls = document.querySelectorAll('.group-card');
    const groupCount = groupEls.length;
    const cols = Math.min(groupCount, 3);
    const rows = Math.ceil(groupCount / cols);
    const cardW = 320;
    const cardH = 260;
    const padding = 40;

    canvas.width = cols * cardW + (cols + 1) * padding;
    canvas.height = rows * cardH + (rows + 1) * padding + 60;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.fillStyle = '#f5f3ff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#4f46e5';
    ctx.font = 'bold 24px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('分组结果', canvas.width / 2, 40);

    const colors = ['#6366f1', '#8b5cf6', '#ec4899', '#f43f5e', '#f97316',
      '#eab308', '#22c55e', '#14b8a6', '#06b6d4', '#3b82f6'];

    groupEls.forEach((groupEl, i) => {
      const col = i % cols;
      const row = Math.floor(i / cols);
      const x = padding + col * (cardW + padding);
      const y = 70 + padding + row * (cardH + padding);

      ctx.fillStyle = colors[i % colors.length];
      ctx.beginPath();
      (ctx as any).roundRect(x, y, cardW, 36, [8, 8, 0, 0]);
      ctx.fill();

      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 14px sans-serif';
      ctx.textAlign = 'left';
      const header = groupEl.querySelector('.group-name');
      ctx.fillText(header?.textContent || '', x + 12, y + 24);

      ctx.fillStyle = '#ffffff';
      ctx.font = '12px sans-serif';
      ctx.textAlign = 'right';
      const count = groupEl.querySelector('.group-count');
      ctx.fillText(count?.textContent || '', x + cardW - 12, y + 24);

      ctx.fillStyle = '#ffffff';
      ctx.beginPath();
      (ctx as any).roundRect(x, y + 36, cardW, cardH - 36, [0, 0, 8, 8]);
      ctx.fill();

      ctx.strokeStyle = '#e5e7eb';
      ctx.lineWidth = 1;
      ctx.beginPath();
      (ctx as any).roundRect(x, y, cardW, cardH, 8);
      ctx.stroke();

      const members = groupEl.querySelectorAll('.member-name');
      members.forEach((member, mi) => {
        ctx.fillStyle = '#1f2937';
        ctx.font = '13px sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText(`${mi + 1}. ${member?.childNodes[0]?.textContent || ''}`, x + 12, y + 60 + mi * 22);
      });
    });

    const link = document.createElement('a');
    link.download = '分组结果.png';
    link.href = canvas.toDataURL();
    link.click();
  };

  const handleExportPrint = () => {
    if (!isHost) return;
    const board = document.querySelector('.group-board');
    if (!board) return;
    const printWindow = window.open('', '_blank');
    if (!printWindow) return;
    printWindow.document.write(`
      <html><head><title>分组结果 - 打印</title>
      <style>
        body { font-family: sans-serif; padding: 20px; }
        h1 { color: #4f46e5; text-align: center; }
        .groups { display: flex; flex-wrap: wrap; gap: 20px; }
        .group { border: 2px solid #e0e7ff; border-radius: 8px; padding: 16px; min-width: 200px; flex: 1; }
        .group h3 { margin: 0 0 8px 0; color: #4f46e5; }
        .group li { padding: 4px 0; list-style: none; }
        @media print { body { padding: 0; } }
      </style></head><body>
      <h1>分组结果</h1>
      <div class="groups">${board.innerHTML}</div>
      </body></html>
    `);
    printWindow.document.close();
    printWindow.print();
  };

  const handleToggleApproval = () => {
    if (!isHost) return;
    socket.setRequireApproval(!requireApproval);
  };

  const handleApprove = (id: string) => {
    socket.approveParticipant(id);
  };

  const handleReject = (id: string) => {
    socket.rejectParticipant(id);
  };

  const handleSaveTemplate = () => {
    if (!isHost || !templateNameInput.trim()) return;
    socket.saveTemplate(templateNameInput.trim());
    setTemplateNameInput('');
    setShowSaveTemplate(false);
  };

  const handleApplyTemplate = (templateId: string) => {
    setConfirmApplyTemplateId(templateId);
  };

  const confirmApplyTemplate = () => {
    if (!confirmApplyTemplateId) return;
    socket.applyTemplate(confirmApplyTemplateId);
    setConfirmApplyTemplateId(null);
  };

  const handleDeleteTemplate = (templateId: string) => {
    if (!isHost) return;
    socket.deleteTemplate(templateId);
  };

  const qrUrl = roomCode ? `${window.location.origin}?room=${roomCode}` : '';

  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);
    const h = date.getHours().toString().padStart(2, '0');
    const m = date.getMinutes().toString().padStart(2, '0');
    const s = date.getSeconds().toString().padStart(2, '0');
    return `${h}:${m}:${s}`;
  };

  const formatDateTime = (timestamp: number) => {
    const date = new Date(timestamp);
    const y = date.getFullYear();
    const mo = (date.getMonth() + 1).toString().padStart(2, '0');
    const d = date.getDate().toString().padStart(2, '0');
    const h = date.getHours().toString().padStart(2, '0');
    const mi = date.getMinutes().toString().padStart(2, '0');
    return `${y}-${mo}-${d} ${h}:${mi}`;
  };

  const getOperatorIcon = (type?: string) => {
    switch (type) {
      case 'host': return '👑';
      case 'self': return '🙋';
      case 'system': return '⚙️';
      default: return '👤';
    }
  };

  const closeAllPanels = () => {
    setShowRules(false);
    setShowHistory(false);
    setShowExport(false);
    setShowQr(false);
    setShowApproval(false);
    setShowTemplates(false);
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
        <button className="secondary-btn" onClick={handleUndo} disabled={logs.length === 0}>
          ↩ 撤销上一步
        </button>
        <button className="secondary-btn" onClick={() => { closeAllPanels(); setShowRules(!showRules); }}>
          📋 分组规则
        </button>
        <button className="secondary-btn" onClick={() => { closeAllPanels(); setShowApproval(!showApproval); }}>
          ✅ 报名审核 {pendingParticipants.length > 0 && <span className="badge">{pendingParticipants.length}</span>}
        </button>
        <button className="secondary-btn" onClick={() => { closeAllPanels(); setShowTemplates(!showTemplates); }}>
          📑 活动模板
        </button>
        <button className="secondary-btn" onClick={() => { closeAllPanels(); setShowHistory(!showHistory); }}>
          📜 历史版本 ({logs.length})
        </button>
        <button className="secondary-btn" onClick={() => { closeAllPanels(); setShowExport(!showExport); }}>
          📤 导出结果
        </button>
        <button className="secondary-btn" onClick={() => { closeAllPanels(); setShowQr(!showQr); }}>
          📱 入场二维码
        </button>
        <button className="secondary-btn" onClick={handleSave}>
          💾 立即保存
        </button>
        {onEnterBigScreen && (
          <button className="secondary-btn" onClick={onEnterBigScreen}>
            📺 主持人大屏
          </button>
        )}
      </div>

      {showQr && roomCode && (
        <div className="qr-section">
          <h4>扫码加入活动</h4>
          <div className="qr-code-box">
            <QRCodeSVG
              value={qrUrl}
              size={180}
              level="H"
              includeMargin={true}
              bgColor="#ffffff"
              fgColor="#4f46e5"
            />
          </div>
          <p className="qr-hint">房间码: <strong>{roomCode}</strong></p>
          <p className="qr-hint">主持人令牌: <strong>{hostToken}</strong></p>
          <p className="qr-url-text">{qrUrl}</p>
          <button className="copy-btn" onClick={() => {
            navigator.clipboard.writeText(qrUrl);
          }}>复制邀请链接</button>
        </div>
      )}

      {showRules && (
        <div className="rules-section">
          <h4>分组规则</h4>
          <label className="rule-check">
            <input type="checkbox" checked={genderBalance} onChange={(e) => setGenderBalance(e.target.checked)} />
            男女比例均衡
          </label>
          <label className="rule-check">
            <input type="checkbox" checked={deptSpread} onChange={(e) => setDeptSpread(e.target.checked)} />
            部门打散
          </label>
          <label className="rule-check">
            <input type="checkbox" checked={skillBalance} onChange={(e) => setSkillBalance(e.target.checked)} />
            能力均衡 (蛇形分配)
          </label>
          <label className="rule-check">
            <input type="checkbox" checked={tagBalance} onChange={(e) => setTagBalance(e.target.checked)} />
            标签均衡
          </label>
          <div className="rule-input-group">
            <label>指定不同组 (名字对，分号分隔)</label>
            <input
              type="text"
              value={separateInput}
              onChange={(e) => setSeparateInput(e.target.value)}
              placeholder="如: 张三,李四; 王五,赵六"
            />
          </div>

          <div className="rule-subsection">
            <h5>固定成员到某组</h5>
            <div className="rule-select-row">
              <select
                value={assignSelectName}
                onChange={(e) => setAssignSelectName(e.target.value)}
                className="name-select"
              >
                <option value="">选择参与者</option>
                {availableParticipants.map((p) => (
                  <option key={p.id} value={p.name}>{p.name}</option>
                ))}
              </select>
              <select
                value={assignSelectGroupId}
                onChange={(e) => setAssignSelectGroupId(e.target.value)}
                className="group-select"
              >
                <option value="">选择目标组</option>
                {groups.map((g) => (
                  <option key={g.id} value={g.id}>{g.name}</option>
                ))}
              </select>
              <button className="add-btn" onClick={handleAddAssignRule}>添加固定规则</button>
            </div>
            {assignRulesList.length > 0 && (
              <div className="rule-list">
                {assignRulesList.map((rule) => {
                  const group = groups.find((g) => g.id === rule.groupId);
                  return (
                    <div key={rule.id} className="rule-list-item">
                      <span>{rule.name} → {group?.name || rule.groupId}</span>
                      <button className="remove-btn" onClick={() => handleRemoveAssignRule(rule.id)}>✕</button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          <div className="rule-subsection">
            <h5>指定成员同组</h5>
            <div className="multi-select-container">
              {availableParticipants.map((p) => (
                <label key={p.id} className="multi-select-item">
                  <input
                    type="checkbox"
                    checked={togetherSelectedIds.includes(p.id)}
                    onChange={() => handleToggleTogetherSelect(p.id)}
                  />
                  <span>{p.name}</span>
                </label>
              ))}
            </div>
            <button
              className="add-btn"
              onClick={handleAddTogetherRule}
              disabled={togetherSelectedIds.length < 2}
            >
              绑定为同组
            </button>
            {togetherRulesList.length > 0 && (
              <div className="rule-list">
                {togetherRulesList.map((rule) => (
                  <div key={rule.id} className="rule-list-item">
                    <span>{getParticipantNames(rule.participantIds)}</span>
                    <button className="remove-btn" onClick={() => handleRemoveTogetherRule(rule.id)}>✕</button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="rule-size-inputs">
            <div className="rule-input-group half">
              <label>每组最少人数</label>
              <input
                type="number"
                min="0"
                value={minSizeInput}
                onChange={(e) => setMinSizeInput(e.target.value)}
                placeholder="默认 0"
              />
            </div>
            <div className="rule-input-group half">
              <label>每组最多人数</label>
              <input
                type="number"
                min="0"
                value={maxSizeInput}
                onChange={(e) => setMaxSizeInput(e.target.value)}
                placeholder="默认 0"
              />
            </div>
          </div>

          <button className="btn-primary" onClick={handleApplyRules}>应用规则</button>
        </div>
      )}

      {showApproval && (
        <div className="approval-section">
          <h4>报名审核</h4>
          <label className="rule-check toggle-switch">
            <input
              type="checkbox"
              checked={requireApproval}
              onChange={handleToggleApproval}
            />
            开启报名审核 {requireApproval ? '(已开启)' : '(未开启)'}
          </label>
          <div className="approval-list">
            <h5>待审核列表 ({pendingParticipants.length})</h5>
            {pendingParticipants.length === 0 ? (
              <div className="empty-history">暂无待审核报名</div>
            ) : (
              <ul>
                {pendingParticipants.map((p) => {
                  const dups = getDuplicates(p);
                  const isDuplicate = dups.length > 0;
                  return (
                    <li key={p.id} className={`approval-item ${isDuplicate ? 'is-duplicate' : ''}`}>
                      <div className="approval-info">
                        <span className="approval-name">
                          {p.gender === '男' ? '👨' : p.gender === '女' ? '👩' : '👤'} {p.name}
                        </span>
                        <span className="approval-meta">
                          {p.gender && <span className="tag">{p.gender}</span>}
                          {p.department && <span className="tag dept">{p.department}</span>}
                          {p.selfRegistered && <span className="tag self">自助报名</span>}
                        </span>
                        {isDuplicate && (
                          <span className="dup-warning" title="重复报名已拒绝">⚠️ 重复报名</span>
                        )}
                      </div>
                      <div className="approval-actions">
                        <button
                          className="approve-btn"
                          onClick={() => handleApprove(p.id)}
                          disabled={isDuplicate}
                        >
                          通过
                        </button>
                        <button
                          className="reject-btn"
                          onClick={() => handleReject(p.id)}
                        >
                          拒绝
                        </button>
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>

          <div className="approval-list rejected-list">
            <h5 className="collapsible-header" onClick={() => setShowRejected(!showRejected)}>
              {showRejected ? '▼' : '▶'} 已拒绝列表 ({rejectedParticipants.length})
            </h5>
            {showRejected && (
              rejectedParticipants.length === 0 ? (
                <div className="empty-history">暂无已拒绝报名</div>
              ) : (
                <ul>
                  {rejectedParticipants.map((p) => (
                    <li key={p.id} className="approval-item rejected">
                      <div className="approval-info">
                        <span className="approval-name">
                          {p.gender === '男' ? '👨' : p.gender === '女' ? '👩' : '👤'} {p.name}
                        </span>
                        <span className="approval-meta">
                          {p.gender && <span className="tag">{p.gender}</span>}
                          {p.department && <span className="tag dept">{p.department}</span>}
                          {p.selfRegistered && <span className="tag self">自助报名</span>}
                        </span>
                      </div>
                      <div className="approval-actions">
                        <button
                          className="approve-btn"
                          onClick={() => handleApprove(p.id)}
                        >
                          恢复
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )
            )}
          </div>
        </div>
      )}

      {showTemplates && (
        <div className="templates-section">
          <h4>活动模板</h4>
          <div className="template-actions">
            {!showSaveTemplate ? (
              <button className="btn-primary" onClick={() => setShowSaveTemplate(true)}>
                💾 保存当前为模板
              </button>
            ) : (
              <div className="save-template-box">
                <input
                  type="text"
                  value={templateNameInput}
                  onChange={(e) => setTemplateNameInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSaveTemplate()}
                  placeholder="输入模板名称"
                  autoFocus
                />
                <div className="save-template-actions">
                  <button className="btn-primary" onClick={handleSaveTemplate}>保存</button>
                  <button className="btn-secondary" onClick={() => { setShowSaveTemplate(false); setTemplateNameInput(''); }}>取消</button>
                </div>
              </div>
            )}
          </div>

          <div className="templates-list">
            <h5>我的模板</h5>
            {localTemplates.length === 0 ? (
              <div className="empty-history">暂无模板</div>
            ) : (
              <ul>
                {localTemplates.map((t) => (
                  <li key={t.id} className="template-item">
                    <div className="template-info">
                      <span className="template-name">{t.name}</span>
                      <span className="template-meta">{t.activityName} · {t.groupCount}组</span>
                    </div>
                    <div className="template-actions">
                      <button className="apply-btn" onClick={() => handleApplyTemplate(t.id)}>应用</button>
                      <button className="delete-btn" onClick={() => handleDeleteTemplate(t.id)}>删除</button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}

      {showHistory && (
        <div className="history-section">
          <h4>历史版本</h4>
          {logs.length === 0 ? (
            <div className="empty-history">暂无历史记录</div>
          ) : (
            <ul className="history-list">
              {logs.map((log, idx) => (
                <li key={idx} className="history-item">
                  <div className="history-header">
                    <span className="history-time">{formatTime(log.timestamp)}</span>
                    <span className="history-operator">{getOperatorIcon(log.operatorType)} {log.operatorName || '系统'}</span>
                  </div>
                  <div className="history-desc">{log.description}</div>
                  <button
                    className="restore-btn"
                    onClick={() => handleRestoreVersion(idx)}
                  >
                    恢复到此版本
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {showExport && (
        <div className="export-section">
          <h4>导出结果</h4>
          <button className="export-btn" onClick={handleExportCsv}>📊 导出 CSV</button>
          <button className="export-btn" onClick={handleExportImage}>🖼️ 导出图片</button>
          <button className="export-btn" onClick={handleExportPrint}>🖨️ 打印结果</button>
        </div>
      )}

      {confirmApplyTemplateId && (
        <div className="modal-overlay" onClick={() => setConfirmApplyTemplateId(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h4>确认应用模板</h4>
            <p>应用模板将覆盖当前的活动设置和分组规则，确定要继续吗？</p>
            <div className="modal-actions">
              <button className="btn-primary" onClick={confirmApplyTemplate}>确认应用</button>
              <button className="btn-secondary" onClick={() => setConfirmApplyTemplateId(null)}>取消</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default HostControls;
