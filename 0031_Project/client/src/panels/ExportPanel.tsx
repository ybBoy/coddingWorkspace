import { useState } from 'react';
import { wsClient } from '../base/wsClient';

export function ExportPanel() {
  const [exporting, setExporting] = useState<string | null>(null);

  const handleExportStayRecords = async () => {
    setExporting('stay');
    try {
      wsClient.exportStayRecords();
    } finally {
      setTimeout(() => setExporting(null), 1000);
    }
  };

  const handleExportLogs = async () => {
    setExporting('logs');
    try {
      wsClient.exportLogs();
    } finally {
      setTimeout(() => setExporting(null), 1000);
    }
  };

  return (
    <div className="export-panel">
      <h3 className="panel-title">数据导出</h3>
      <div className="export-buttons">
        <button
          className="btn btn-success"
          onClick={handleExportStayRecords}
          disabled={exporting !== null}
        >
          {exporting === 'stay' ? '导出中...' : '📊 导出入住记录 CSV'}
        </button>
        <button
          className="btn btn-info"
          onClick={handleExportLogs}
          disabled={exporting !== null}
        >
          {exporting === 'logs' ? '导出中...' : '📝 导出操作日志 CSV'}
        </button>
      </div>
      <p className="export-hint">
        CSV 文件可用 Excel 打开，包含完整字段，方便练习报表制作
      </p>
    </div>
  );
}
