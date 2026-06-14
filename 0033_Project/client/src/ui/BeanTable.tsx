import React from 'react';
import { CoffeeBean, StockRecord } from '../types';
import { getRoastLabel } from '../constants/roastLevels';

interface BeanTableProps {
  beans: CoffeeBean[];
  selectedBeanId: string | null;
  onSelectBean: (bean: CoffeeBean) => void;
  onEdit: (bean: CoffeeBean) => void;
  onDelete: (id: string) => void;
  recentRecords: StockRecord[];
}

const getWarningClass = (bean: CoffeeBean): string => {
  if (bean.stockGrams <= 0) return 'warning-empty';
  if (bean.stockGrams <= bean.minStockLevel) return 'warning-low';
  const ratio = bean.stockGrams / bean.minStockLevel;
  if (ratio <= 1.5) return 'warning-approaching';
  return '';
};

const getWarningLabel = (bean: CoffeeBean): string => {
  if (bean.stockGrams <= 0) return '已耗尽';
  if (bean.stockGrams <= bean.minStockLevel) return '已不足';
  const ratio = bean.stockGrams / bean.minStockLevel;
  if (ratio <= 1.5) return '即将不足';
  return '正常';
};

const BeanTable: React.FC<BeanTableProps> = ({
  beans,
  selectedBeanId,
  onSelectBean,
  onEdit,
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
            <th>预警状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          {beans.map((bean) => {
            const warnClass = getWarningClass(bean);
            const warnLabel = getWarningLabel(bean);
            const isSelected = selectedBeanId === bean.id;
            return (
              <tr
                key={bean.id}
                className={`bean-row ${warnClass ? `row-${warnClass}` : ''} ${isSelected ? 'selected-row' : ''}`}
                onClick={() => onSelectBean(bean)}
              >
                <td className="bean-name">{bean.name}</td>
                <td>{bean.origin}</td>
                <td>
                  <span className={`roast-badge roast-${bean.roastLevel.toLowerCase()}`}>
                    {getRoastLabel(bean.roastLevel)}
                  </span>
                </td>
                <td className={`stock-amount ${bean.stockGrams <= bean.minStockLevel ? 'low' : ''} ${bean.stockGrams <= 0 ? 'empty' : ''}`}>
                  {bean.stockGrams}
                </td>
                <td>{bean.minStockLevel}</td>
                <td>
                  <span className={`status-badge ${warnClass ? `status-${warnClass.split('-')[1]}` : 'status-normal'}`}>
                    {warnLabel}
                  </span>
                </td>
                <td className="action-cell">
                  <button
                    className="btn btn-small btn-secondary"
                    onClick={(e) => { e.stopPropagation(); onEdit(bean); }}
                  >
                    编辑
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
              <td colSpan={7} className="empty-row">暂无咖啡豆数据，请新增或调整搜索条件</td>
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
                <th>操作人</th>
                <th>变动 (克)</th>
                <th>变动前</th>
                <th>变动后</th>
                <th>备注</th>
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
                  <td className="operator-cell">{record.operator || '-'}</td>
                  <td className={record.type === 'CONSUME' ? 'consume-amount' : record.type === 'INIT' ? '' : 'restock-amount'}>
                    {record.type === 'CONSUME' ? '-' : record.type === 'INIT' ? '' : '+'}{record.quantity}
                  </td>
                  <td>{record.beforeStock}</td>
                  <td><strong>{record.afterStock}</strong></td>
                  <td className="remark-cell">{record.remark || '-'}</td>
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
