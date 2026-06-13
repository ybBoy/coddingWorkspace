import React, { useState } from 'react';
import { AddBeanRequest } from '../types';

interface BeanFormProps {
  onSubmit: (bean: AddBeanRequest) => void;
}

const ROAST_LEVELS = ['浅烘焙', '中烘焙', '中深烘焙', '深烘焙'];

const BeanForm: React.FC<BeanFormProps> = ({ onSubmit }) => {
  const [name, setName] = useState('');
  const [origin, setOrigin] = useState('');
  const [roastLevel, setRoastLevel] = useState('中烘焙');
  const [stockGrams, setStockGrams] = useState(0);
  const [minStockLevel, setMinStockLevel] = useState(500);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !origin.trim()) return;
    onSubmit({
      name: name.trim(),
      origin: origin.trim(),
      roastLevel,
      stockGrams,
      minStockLevel,
    });
    setName('');
    setOrigin('');
    setRoastLevel('中烘焙');
    setStockGrams(0);
    setMinStockLevel(500);
  };

  return (
    <div className="bean-form-card">
      <h3>新增咖啡豆</h3>
      <form onSubmit={handleSubmit} className="bean-form">
        <div className="form-group">
          <label>豆子名称</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="如：埃塞俄比亚 耶加雪菲"
            required
          />
        </div>
        <div className="form-group">
          <label>产地</label>
          <input
            type="text"
            value={origin}
            onChange={(e) => setOrigin(e.target.value)}
            placeholder="如：埃塞俄比亚"
            required
          />
        </div>
        <div className="form-group">
          <label>烘焙程度</label>
          <select value={roastLevel} onChange={(e) => setRoastLevel(e.target.value)}>
            {ROAST_LEVELS.map((level) => (
              <option key={level} value={level}>{level}</option>
            ))}
          </select>
        </div>
        <div className="form-row">
          <div className="form-group">
            <label>初始库存 (克)</label>
            <input
              type="number"
              min="0"
              value={stockGrams}
              onChange={(e) => setStockGrams(Number(e.target.value))}
            />
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
        </div>
        <button type="submit" className="btn btn-primary">
          添加咖啡豆
        </button>
      </form>
    </div>
  );
};

export default BeanForm;
