import React, { useState, useEffect } from 'react';
import { CoffeeBean } from '../types';
import { ROAST_LEVELS } from '../constants/roastLevels';

interface EditBeanDialogProps {
  bean: CoffeeBean | null;
  onClose: () => void;
  onSave: (id: string, req: any) => void;
}

const EditBeanDialog: React.FC<EditBeanDialogProps> = ({ bean, onClose, onSave }) => {
  const [name, setName] = useState('');
  const [origin, setOrigin] = useState('');
  const [roastLevel, setRoastLevel] = useState('MEDIUM');
  const [minStockLevel, setMinStockLevel] = useState(0);
  const [operator, setOperator] = useState('');

  useEffect(() => {
    if (bean) {
      setName(bean.name);
      setOrigin(bean.origin);
      setRoastLevel(bean.roastLevel);
      setMinStockLevel(bean.minStockLevel);
      setOperator('');
    }
  }, [bean]);

  if (!bean) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const req: any = {};
    if (name.trim() !== bean.name) req.name = name.trim();
    if (origin.trim() !== bean.origin) req.origin = origin.trim();
    if (roastLevel !== bean.roastLevel) req.roastLevel = roastLevel;
    if (minStockLevel !== bean.minStockLevel) req.minStockLevel = minStockLevel;
    if (Object.keys(req).length === 0) {
      onClose();
      return;
    }
    if (operator.trim()) req.operator = operator.trim();
    onSave(bean.id, req);
    onClose();
  };

  return (
    <div className="dialog-overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-header">
          <h3>编辑咖啡豆资料</h3>
          <button className="dialog-close" onClick={onClose}>×</button>
        </div>
        <div className="dialog-body">
          <div className="edit-bean-id">ID: {bean.id}</div>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>豆子名称</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label>产地</label>
              <input
                type="text"
                value={origin}
                onChange={(e) => setOrigin(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label>烘焙程度</label>
              <select value={roastLevel} onChange={(e) => setRoastLevel(e.target.value)}>
                {ROAST_LEVELS.map((level) => (
                  <option key={level.code} value={level.code}>
                    {level.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>最低库存线 (克)</label>
              <input
                type="number"
                min="0"
                value={minStockLevel}
                onChange={(e) => setMinStockLevel(Number(e.target.value))}
              />
            </div>
            <div className="form-group">
              <label>操作人</label>
              <input
                type="text"
                placeholder="如：张三 (可选)"
                value={operator}
                onChange={(e) => setOperator(e.target.value)}
              />
            </div>
            <div className="form-group info-only">
              <label>当前库存 (克) - 不可编辑</label>
              <input type="text" value={`${bean.stockGrams} 克`} disabled />
            </div>
            <div className="dialog-actions">
              <button type="button" className="btn btn-secondary" onClick={onClose}>
                取消
              </button>
              <button type="submit" className="btn btn-primary">
                保存修改
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default EditBeanDialog;
