import React, { useState, useEffect, useCallback } from 'react';
import { CoffeeBean, StockRecord, AddBeanRequest } from '../types';
import { beanService } from '../services/beanService';
import LowStockBanner from '../ui/LowStockBanner';
import BeanForm from '../ui/BeanForm';
import BeanTable from '../ui/BeanTable';
import StockAdjustDialog from '../ui/StockAdjustDialog';

const ROAST_LEVELS = ['全部', '浅烘焙', '中烘焙', '中深烘焙', '深烘焙'];

const InventoryView: React.FC = () => {
  const [beans, setBeans] = useState<CoffeeBean[]>([]);
  const [lowStockCount, setLowStockCount] = useState(0);
  const [selectedBean, setSelectedBean] = useState<CoffeeBean | null>(null);
  const [recentRecords, setRecentRecords] = useState<StockRecord[]>([]);
  const [adjustBean, setAdjustBean] = useState<CoffeeBean | null>(null);
  const [filterRoast, setFilterRoast] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const loadBeans = useCallback(async () => {
    try {
      setLoading(true);
      const data = await beanService.getAllBeans(filterRoast || undefined);
      setBeans(data);
      const count = await beanService.getLowStockCount();
      setLowStockCount(count);
    } catch (err) {
      setError('加载数据失败，请确认后端服务是否启动');
    } finally {
      setLoading(false);
    }
  }, [filterRoast]);

  useEffect(() => {
    loadBeans();
  }, [loadBeans]);

  const handleSelectBean = async (bean: CoffeeBean) => {
    if (selectedBean?.id === bean.id) {
      setSelectedBean(null);
      setRecentRecords([]);
      return;
    }
    setSelectedBean(bean);
    try {
      const records = await beanService.getRecentRecords(bean.id, 5);
      setRecentRecords(records);
    } catch (err) {
      setRecentRecords([]);
    }
  };

  const handleAddBean = async (beanReq: AddBeanRequest) => {
    try {
      await beanService.addBean(beanReq);
      loadBeans();
    } catch (err) {
      setError('添加咖啡豆失败');
    }
  };

  const handleRestock = async (id: string, amount: number) => {
    try {
      const updated = await beanService.restockBean(id, amount);
      loadBeans();
      if (selectedBean?.id === id) {
        setSelectedBean(updated);
        const records = await beanService.getRecentRecords(id, 5);
        setRecentRecords(records);
      }
    } catch (err) {
      setError('补货失败');
    }
  };

  const handleConsume = async (id: string, amount: number) => {
    try {
      const updated = await beanService.consumeBean(id, amount);
      loadBeans();
      if (selectedBean?.id === id) {
        setSelectedBean(updated);
        const records = await beanService.getRecentRecords(id, 5);
        setRecentRecords(records);
      }
    } catch (err) {
      setError('消耗记录失败');
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('确定要删除这款咖啡豆吗？')) return;
    try {
      await beanService.deleteBean(id);
      if (selectedBean?.id === id) {
        setSelectedBean(null);
        setRecentRecords([]);
      }
      loadBeans();
    } catch (err) {
      setError('删除失败');
    }
  };

  return (
    <div className="inventory-view">
      <header className="app-header">
        <h1>☕ 咖啡豆库存管理系统</h1>
        <p className="subtitle">Coffee Bean Inventory Management</p>
      </header>

      <LowStockBanner lowStockCount={lowStockCount} />

      {error && (
        <div className="error-message" onClick={() => setError('')}>
          {error} ×
        </div>
      )}

      <div className="main-content">
        <div className="left-panel">
          <div className="filter-bar">
            <div className="filter-group">
              <label>按烘焙程度筛选：</label>
              <div className="filter-tags">
                {ROAST_LEVELS.map((level) => {
                  const isActive = level === '全部' ? !filterRoast : filterRoast === level;
                  return (
                    <button
                      key={level}
                      className={`filter-tag ${isActive ? 'active' : ''}`}
                      onClick={() => setFilterRoast(level === '全部' ? '' : level)}
                    >
                      {level}
                    </button>
                  );
                })}
              </div>
            </div>
            <div className="bean-count">
              共 <strong>{beans.length}</strong> 款咖啡豆
            </div>
          </div>

          {loading ? (
            <div className="loading">加载中...</div>
          ) : (
            <BeanTable
              beans={beans}
              selectedBeanId={selectedBean?.id || null}
              onSelectBean={handleSelectBean}
              onAdjustStock={setAdjustBean}
              onDelete={handleDelete}
              recentRecords={recentRecords}
            />
          )}
        </div>

        <div className="right-panel">
          <BeanForm onSubmit={handleAddBean} />
        </div>
      </div>

      <StockAdjustDialog
        bean={adjustBean}
        onClose={() => setAdjustBean(null)}
        onRestock={handleRestock}
        onConsume={handleConsume}
      />
    </div>
  );
};

export default InventoryView;
