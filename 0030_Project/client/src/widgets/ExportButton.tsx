import React from 'react';
import { CareRecord, CARE_ACTIONS, STATUS_LABELS } from '../types';

interface ExportButtonProps {
  records: CareRecord[];
}

const ExportButton: React.FC<ExportButtonProps> = ({ records }) => {
  const getActionLabel = (action: string) => {
    const found = CARE_ACTIONS.find((a) => a.value === action);
    return found ? found.label : action;
  };

  const exportCSV = () => {
    const BOM = '\uFEFF';
    const header = '时间,宠物名,动作,备注,操作人\n';
    const rows = records.map((r) =>
      `${r.time},${r.petName},${getActionLabel(r.action)},${r.note || ''},${r.staffName || ''}`
    ).join('\n');
    const blob = new Blob([BOM + header + rows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    const now = new Date();
    const dateStr = `${now.getFullYear()}${(now.getMonth()+1).toString().padStart(2,'0')}${now.getDate().toString().padStart(2,'0')}`;
    link.download = `护理记录_${dateStr}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const handlePrint = () => {
    const printContent = records.map((r) =>
      `<tr><td>${r.time}</td><td>${r.petName}</td><td>${getActionLabel(r.action)}</td><td>${r.note || ''}</td><td>${r.staffName || ''}</td></tr>`
    ).join('');
    const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>护理记录</title>
      <style>body{font-family:system-ui;padding:24px;color:#333}h2{color:#4a8a92}
      table{width:100%;border-collapse:collapse;margin-top:16px}th,td{border:1px solid #ddd;padding:8px;text-align:left;font-size:14px}
      th{background:#f5f5f5}</style></head>
      <body><h2>🐾 萌宠乐园寄养中心 - 今日护理记录</h2>
      <p>导出时间：${new Date().toLocaleString('zh-CN')}</p>
      <table><thead><tr><th>时间</th><th>宠物名</th><th>动作</th><th>备注</th><th>操作人</th></tr></thead>
      <tbody>${printContent}</tbody></table></body></html>`;
    const win = window.open('', '_blank');
    if (win) {
      win.document.write(html);
      win.document.close();
      win.print();
    }
  };

  return (
    <div className="export-buttons">
      <button className="btn-secondary" onClick={exportCSV}>📥 导出CSV</button>
      <button className="btn-secondary" onClick={handlePrint}>🖨️ 打印</button>
    </div>
  );
};

export default ExportButton;
