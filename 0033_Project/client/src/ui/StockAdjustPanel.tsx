import React, { useState } from 'react';
import { CoffeeBean } from '../types';
import { getRoastLabel } from '../constants/roastLevels';

interface StockAdjustPanelProps {
  bean: CoffeeBean | null;
  onRestock: (id: string, amount: number) => void;
  onConsume: (id: string, amount: number) => void;
}

const StockAdjustPanel: React.FC<StockAdjustPanelProps> = ({ bean, onRestock, onConsume }) => {
  const [mode, setMode] = useState<'restock' | 'consume'>('restock');
  const [amount, setAmount] = useState(100);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!bean) return;
    if (amount <= 0) {
      alert('数量必须大于0');
      return;
    }
    if (mode === 'restock') {
      onRestock(bean.id, amount);
    } else {
      if (amount > bean.stockGrams) {
        alert('消耗数量不能大于当前库存');
        return;
      }
      onConsume(bean.id, amount);
    }
    setAmount(100);
  };

  if (!bean) {
    return (
      <div className="stock-adjust-panel empty">
        <h3>库存调整</h3>
        <div className="empty-hint">
          请在左侧表格中点击一款咖啡豆，
          <br />
          在此处进行库存调整
        </div>
      </div>
    );
  }

  return (
    <div className="stock-adjust-panel">
      <h3>库存调整</h3>

      <div className="selected-bean-info">
        <div className="bean-name-large">{bean.name}</div>
        <div className="bean-meta">
          <span className="meta-item">
            <span className="meta-label">产地</span>
            <span className="meta-value">{bean.origin}</span>
          </span>
          <span className="meta-item">
            <span className="meta-label">烘焙</span>
            <span className="meta-value">{getRoastLabel(bean.roastLevel)}</span>
          </span>
        </div>
        <div className="stock-display">
          <div className="stock-item">
            <span className="stock-label">当前库存</span>
            <span className={`stock-value ${bean.stockGrams <= bean.minStockLevel ? 'low' : ''}`}>
              {bean.stockGrams} 克
            </span>
          </div>
          <div className="stock-item">
            <span className="stock-label">最低库存线</span>
            <span className="stock-value">{bean.minStockLevel} 克</span>
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="adjust-form">
        <div className="mode-tabs">
          <button
            type="button"
            className={`tab-btn ${mode === 'restock' ? 'active' : ''}`}
            onClick={() => setMode('restock')}
          >
            补货 +
          </button>
          <button
            type="button"
            className={`tab-btn ${mode === 'consume' ? 'active' : ''}`}
            onClick={() => setMode('consume')}
          >
            消耗 -
          </button>
        </div>

        <div className="form-group">
          <label>{mode === 'restock' ? '补货数量 (克)' : '消耗数量 (克)'}</label>
          <input
            type="number"
            min="1"
            max={mode === 'consume' ? bean.stockGrams : undefined}
            value={amount}
            onChange={(e) => setAmount(Number(e.target.value))}
            autoFocus
          />
        </div>

        <div className="quick-amounts">
          <span className="quick-label">快速选择：</span>
          {[50, 100, 200, 500].map((num) => (
            <button
              key={num}
              type="button"
              className={`quick-btn ${amount === num ? 'active' : ''}`}
              onClick={() => setAmount(num)}
            >
              {num}
            </button>
          ))}
        </div>

        <button
          type="submit"
          className={`btn btn-full ${mode === 'restock' ? 'btn-primary' : 'btn-warning'}`}
        >
          确认{mode === 'restock' ? '补货' : '消耗'} {amount} 克
        </button>
      </form>
    </div>
  );
};

export default StockAdjustPanel;
