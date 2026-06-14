import React, { useRef, useState } from 'react';
import { CoffeeBean } from '../types';
import { beanService } from '../services/beanService';

interface ImportExportPanelProps {
  onRefresh: () => void;
}

const ImportExportPanel: React.FC<ImportExportPanelProps> = ({ onRefresh }) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [replaceMode, setReplaceMode] = useState(false);
  const [message, setMessage] = useState('');

  const handleExportJson = () => {
    window.open(beanService.exportJsonUrl(), '_blank');
  };

  const handleExportCsv = () => {
    window.open(beanService.exportCsvUrl(), '_blank');
  };

  const triggerImport = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setMessage('');
    try {
      const text = await file.text();
      const data: CoffeeBean[] = JSON.parse(text);
      if (!Array.isArray(data)) {
        throw new Error('JSON 格式不正确，应该是数组');
      }
      await beanService.importBeans(data, replaceMode);
      setMessage(`✅ 成功导入 ${data.length} 条数据`);
      onRefresh();
    } catch (err: any) {
      setMessage(`❌ 导入失败：${err.message || '未知错误'}`);
    } finally {
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  return (
    <div className="io-panel">
      <h3>📦 数据备份 / 批量导入</h3>

      <div className="io-section">
        <h4>导出数据</h4>
        <div className="io-actions">
          <button className="btn btn-small btn-secondary" onClick={handleExportJson}>
            导出 JSON
          </button>
          <button className="btn btn-small btn-secondary" onClick={handleExportCsv}>
            导出 CSV
          </button>
        </div>
      </div>

      <div className="io-section">
        <h4>导入 JSON</h4>
        <div className="import-options">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={replaceMode}
              onChange={(e) => setReplaceMode(e.target.checked)}
            />
            <span>覆盖现有数据（否则追加）</span>
          </label>
        </div>
        <div className="io-actions">
          <button className="btn btn-small btn-primary" onClick={triggerImport}>
            选择文件导入
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".json"
            style={{ display: 'none' }}
            onChange={handleFileChange}
          />
        </div>
      </div>

      {message && (
        <div className={`io-message ${message.startsWith('✅') ? 'success' : 'error'}`}>
          {message}
        </div>
      )}
    </div>
  );
};

export default ImportExportPanel;
