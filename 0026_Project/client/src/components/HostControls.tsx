import React, { useState, useEffect, useRef } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import socket from '../shared/socket';
import { ActionLog, GroupRule } from '../shared/types';

interface HostControlsProps {
  isHost: boolean;
  logs: ActionLog[];
  rules: GroupRule[];
  roomCode: string;
  hostToken: string;
  onClaimHost: (token: string) => void;
}

const HostControls: React.FC<HostControlsProps> = ({
  isHost, logs, rules, roomCode, hostToken, onClaimHost,
}) => {
  const [tokenInput, setTokenInput] = useState('');
  const [showRules, setShowRules] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [showExport, setShowExport] = useState(false);
  const [showQr, setShowQr] = useState(false);

  const [genderBalance, setGenderBalance] = useState(false);
  const [deptSpread, setDeptSpread] = useState(false);
  const [skillBalance, setSkillBalance] = useState(false);
  const [separateInput, setSeparateInput] = useState('');

  useEffect(() => {
    setGenderBalance(rules.some((r) => r.type === 'gender-balance'));
    setDeptSpread(rules.some((r) => r.type === 'dept-spread'));
    setSkillBalance(rules.some((r) => r.type === 'skill-balance'));
    const sepRule = rules.find((r) => r.type === 'separate');
    setSeparateInput(sepRule ? sepRule.value : '');
  }, [rules]);

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

  const handleApplyRules = () => {
    if (!isHost) return;
    const newRules: GroupRule[] = [];
    if (genderBalance) newRules.push({ type: 'gender-balance', value: 'true' });
    if (deptSpread) newRules.push({ type: 'dept-spread', value: 'true' });
    if (skillBalance) newRules.push({ type: 'skill-balance', value: 'true' });
    if (separateInput.trim()) {
      separateInput.split(/[;\n]/).filter((s) => s.trim()).forEach((pair) => {
        newRules.push({ type: 'separate', value: pair.trim() });
      });
    }
    socket.send({ type: 'set-rules', rules: newRules });
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
    const groups = document.querySelectorAll('.group-card');
    const groupCount = groups.length;
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

    groups.forEach((groupEl, i) => {
      const col = i % cols;
      const row = Math.floor(i / cols);
      const x = padding + col * (cardW + padding);
      const y = 70 + padding + row * (cardH + padding);

      ctx.fillStyle = colors[i % colors.length];
      ctx.beginPath();
      ctx.roundRect(x, y, cardW, 36, [8, 8, 0, 0]);
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
      ctx.roundRect(x, y + 36, cardW, cardH - 36, [0, 0, 8, 8]);
      ctx.fill();

      ctx.strokeStyle = '#e5e7eb';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.roundRect(x, y, cardW, cardH, 8);
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

  const qrUrl = roomCode ? `${window.location.origin}?room=${roomCode}` : '';

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
        <button className="secondary-btn" onClick={handleUndo} disabled={logs.length === 0}>
          ↩ 撤销上一步
        </button>
        <button className="secondary-btn" onClick={() => setShowRules(!showRules)}>
          📋 分组规则
        </button>
        <button className="secondary-btn" onClick={() => setShowHistory(!showHistory)}>
          📜 历史版本 ({logs.length})
        </button>
        <button className="secondary-btn" onClick={() => setShowExport(!showExport)}>
          📤 导出结果
        </button>
        <button className="secondary-btn" onClick={() => setShowQr(!showQr)}>
          📱 入场二维码
        </button>
        <button className="secondary-btn" onClick={handleSave}>
          💾 立即保存
        </button>
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
          <div className="rule-input-group">
            <label>指定不同组 (名字对，分号分隔)</label>
            <input
              type="text"
              value={separateInput}
              onChange={(e) => setSeparateInput(e.target.value)}
              placeholder="如: 张三,李四; 王五,赵六"
            />
          </div>
          <button className="btn-primary" onClick={handleApplyRules}>应用规则</button>
        </div>
      )}

      {showHistory && (
        <div className="history-section">
          <h4>历史版本</h4>
          <div className="history-list">
            {logs.length === 0 ? (
              <div className="empty-history">暂无操作记录</div>
            ) : (
              <ul>
                {[...logs].reverse().map((log, index) => (
                  <li key={index} className="history-item">
                    <div className="history-info">
                      <span className="history-time">{formatTime(log.timestamp)}</span>
                      <span className="history-desc">{log.description}</span>
                    </div>
                    <button
                      className="restore-btn"
                      onClick={() => handleRestoreVersion(logs.length - 1 - index)}
                    >
                      恢复
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}

      {showExport && (
        <div className="export-section">
          <h4>导出结果</h4>
          <div className="export-buttons">
            <button className="export-btn" onClick={handleExportCsv}>
              📊 导出 CSV
            </button>
            <button className="export-btn" onClick={handleExportImage}>
              🖼 导出图片
            </button>
            <button className="export-btn" onClick={handleExportPrint}>
              🖨 打印名单
            </button>
          </div>
        </div>
      )}

      {!showRules && !showHistory && !showExport && !showQr && (
        <div className="host-tip">
          <p>💡 提示：</p>
          <ul>
            <li>点选成员再点目标组即可移动</li>
            <li>锁定组后重新分组，该组人员不变</li>
            <li>可设置分组规则后再随机分组</li>
            <li>数据每30秒自动保存一次</li>
          </ul>
        </div>
      )}
    </div>
  );
};

export default HostControls;
