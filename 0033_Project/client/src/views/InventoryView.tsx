import React, { useState, useEffect, useCallback } from 'react';
import { CoffeeBean, StockRecord, AddBeanRequest } from '../types';
import { beanService } from '../services/beanService';
import { FILTER_OPTIONS } from '../constants/roastLevels';
import LowStockBanner from '../ui/LowStockBanner';
import BeanForm from '../ui/BeanForm';
import BeanTable from '../ui/BeanTable';
import StockAdjustPanel from '../ui/StockAdjustPanel';

const InventoryView: React.FC = () => {
  const [beans, setBeans] = useState<CoffeeBean[]>([]);
  const [lowStockCount, setLowStockCount] = useState(0);
  const [selectedBean, setSelectedBean] = useState<CoffeeBean | null>(null);
  const [recentRecords, setRecentRecords] = useState<StockRecord[]>([]);
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
      if (selectedBean) {
        const updated = data.find((b) => b.id === selectedBean.id);
        if (updated) {
          setSelectedBean(updated);
        }
      }
    } catch (err) {
      setError('加载数据失败，请确认后端服务是否启动');
    } finally {
      setLoading(false);
    }
  }, [filterRoast, selectedBean]);

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
      await beanService.restockBean(id, amount);
      loadBeans();
      if (selectedBean?.id === id) {
        const records = await beanService.getRecentRecords(id, 5);
        setRecentRecords(records);
      }
    } catch (err) {
      setError('补货失败');
    }
  };

  const handleConsume = async (id: string, amount: number) => {
    try {
      await beanService.consumeBean(id, amount);
      loadBeans();
      if (selectedBean?.id === id) {
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
                {FILTER_OPTIONS.map((option) => {
                  const isActive =
                    option.code === '' ? !filterRoast : filterRoast === option.code;
                  return (
                    <button
                      key={option.code || 'all'}
                      className={`filter-tag ${isActive ? 'active' : ''}`}
                      onClick={() => setFilterRoast(option.code)}
                    >
                      {option.label}
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
              onDelete={handleDelete}
              recentRecords={recentRecords}
            />
          )}
        </div>

        <div className="right-panel">
          <BeanForm onSubmit={handleAddBean} />
          <StockAdjustPanel
            bean={selectedBean}
            onRestock={handleRestock}
            onConsume={handleConsume}
          />
        </div>
      </div>
    </div>
  );
};

export default InventoryView;
