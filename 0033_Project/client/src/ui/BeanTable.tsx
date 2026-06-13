import React from 'react';
import { CoffeeBean, StockRecord } from '../types';

interface BeanTableProps {
  beans: CoffeeBean[];
  selectedBeanId: string | null;
  onSelectBean: (bean: CoffeeBean) => void;
  onAdjustStock: (bean: CoffeeBean) => void;
  onDelete: (id: string) => void;
  recentRecords: StockRecord[];
}

const BeanTable: React.FC<BeanTableProps> = ({
  beans,
  selectedBeanId,
  onSelectBean,
  onAdjustStock,
  onDelete,
  recentRecords,
}) => {
  const getRecordTypeLabel = (type: string) => {
    switch (type) {
      case 'RESTOCK': return '补货';
      case 'CONSUME': return '消耗';
      case 'INIT': return '初始';
      default: return type;
    }
  };

  const getRecordTypeClass = (type: string) => {
    switch (type) {
      case 'RESTOCK': return 'record-restock';
      case 'CONSUME': return 'record-consume';
      case 'INIT': return 'record-init';
      default: return '';
    }
  };

  return (
    <div className="bean-table-wrapper">
      <table className="bean-table">
        <thead>
          <tr>
            <th>豆子名称</th>
            <th>产地</th>
            <th>烘焙程度</th>
            <th>当前库存 (克)</th>
            <th>最低库存线 (克)</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          {beans.map((bean) => {
            const isLow = bean.stockGrams <= bean.minStockLevel;
            const isSelected = selectedBeanId === bean.id;
            return (
              <tr
                key={bean.id}
                className={`bean-row ${isLow ? 'low-stock-row' : ''} ${isSelected ? 'selected-row' : ''}`}
                onClick={() => onSelectBean(bean)}
              >
                <td className="bean-name">{bean.name}</td>
                <td>{bean.origin}</td>
                <td>
                  <span className={`roast-badge roast-${bean.roastLevel}`}>
                    {bean.roastLevel}
                  </span>
                </td>
                <td className={`stock-amount ${isLow ? 'low' : ''}`}>
                  {bean.stockGrams}
                </td>
                <td>{bean.minStockLevel}</td>
                <td>
                  {isLow ? (
                    <span className="status-badge status-low">库存不足</span>
                  ) : (
                    <span className="status-badge status-normal">正常</span>
                  )}
                </td>
                <td className="action-cell">
                  <button
                    className="btn btn-small btn-secondary"
                    onClick={(e) => { e.stopPropagation(); onAdjustStock(bean); }}
                  >
                    库存调整
                  </button>
                  <button
                    className="btn btn-small btn-danger"
                    onClick={(e) => { e.stopPropagation(); onDelete(bean.id); }}
                  >
                    删除
                  </button>
                </td>
              </tr>
            );
          })}
          {beans.length === 0 && (
            <tr>
              <td colSpan={7} className="empty-row">暂无咖啡豆数据</td>
            </tr>
          )}
        </tbody>
      </table>

      {selectedBeanId && recentRecords.length > 0 && (
        <div className="records-panel">
          <h4>最近 5 条库存变动记录</h4>
          <table className="records-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>类型</th>
                <th>变动数量 (克)</th>
                <th>变动后库存 (克)</th>
              </tr>
            </thead>
            <tbody>
              {recentRecords.map((record) => (
                <tr key={record.id}>
                  <td>{record.timestamp}</td>
                  <td>
                    <span className={`record-type-badge ${getRecordTypeClass(record.type)}`}>
                      {getRecordTypeLabel(record.type)}
                    </span>
                  </td>
                  <td className={record.type === 'CONSUME' ? 'consume-amount' : 'restock-amount'}>
                    {record.type === 'CONSUME' ? '-' : '+'}{record.quantity}
                  </td>
                  <td>{record.remainingStock}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default BeanTable;
