import React, { useState } from 'react';
import { CoffeeBean } from '../types';

interface StockAdjustDialogProps {
  bean: CoffeeBean | null;
  onClose: () => void;
  onRestock: (id: string, amount: number) => void;
  onConsume: (id: string, amount: number) => void;
}

const StockAdjustDialog: React.FC<StockAdjustDialogProps> = ({
  bean,
  onClose,
  onRestock,
  onConsume,
}) => {
  const [mode, setMode] = useState<'restock' | 'consume'>('restock');
  const [amount, setAmount] = useState(100);

  if (!bean) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (amount <= 0) return;
    if (mode === 'restock') {
      onRestock(bean.id, amount);
    } else {
      onConsume(bean.id, amount);
    }
    onClose();
  };

  return (
    <div className="dialog-overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-header">
          <h3>库存调整 - {bean.name}</h3>
          <button className="dialog-close" onClick={onClose}>×</button>
        </div>
        <div className="dialog-body">
          <div className="stock-info">
            <div className="info-item">
              <span className="info-label">当前库存</span>
              <span className="info-value">{bean.stockGrams} 克</span>
            </div>
            <div className="info-item">
              <span className="info-label">最低库存线</span>
              <span className="info-value">{bean.minStockLevel} 克</span>
            </div>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="mode-tabs">
              <button
                type="button"
                className={`tab-btn ${mode === 'restock' ? 'active' : ''}`}
                onClick={() => setMode('restock')}
              >
                补货
              </button>
              <button
                type="button"
                className={`tab-btn ${mode === 'consume' ? 'active' : ''}`}
                onClick={() => setMode('consume')}
              >
                消耗
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

            <div className="dialog-actions">
              <button type="button" className="btn btn-secondary" onClick={onClose}>
                取消
              </button>
              <button
                type="submit"
                className={`btn ${mode === 'restock' ? 'btn-primary' : 'btn-warning'}`}
              >
                确认{mode === 'restock' ? '补货' : '消耗'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default StockAdjustDialog;
