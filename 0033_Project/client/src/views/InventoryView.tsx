import React, { useState, useEffect, useCallback } from 'react';
import { CoffeeBean, StockRecord, AddBeanRequest, WarningSummary, SortField, SortDir } from '../types';
import { beanService } from '../services/beanService';
import LowStockBanner from '../ui/LowStockBanner';
import SearchSortBar from '../ui/SearchSortBar';
import BeanForm from '../ui/BeanForm';
import BeanTable from '../ui/BeanTable';
import StockAdjustPanel from '../ui/StockAdjustPanel';
import StatisticsPanel from '../ui/StatisticsPanel';
import ImportExportPanel from '../ui/ImportExportPanel';
import EditBeanDialog from '../ui/EditBeanDialog';

const InventoryView: React.FC = () => {
  const [beans, setBeans] = useState<CoffeeBean[]>([]);
  const [warningSummary, setWarningSummary] = useState<WarningSummary | null>(null);
  const [selectedBean, setSelectedBean] = useState<CoffeeBean | null>(null);
  const [recentRecords, setRecentRecords] = useState<StockRecord[]>([]);
  const [editBean, setEditBean] = useState<CoffeeBean | null>(null);

  const [search, setSearch] = useState('');
  const [filterRoast, setFilterRoast] = useState('');
  const [sortBy, setSortBy] = useState<SortField>('name');
  const [sortDir, setSortDir] = useState<SortDir>('asc');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const loadBeans = useCallback(async () => {
    try {
      setLoading(true);
      const data = await beanService.getAllBeans(
        filterRoast || undefined,
        search || undefined,
        sortBy,
        sortDir
      );
      setBeans(data);
      try {
        const summary = await beanService.getWarningSummary();
        setWarningSummary(summary);
      } catch (_) {}
      if (selectedBean) {
        const updated = data.find((b) => b.id === selectedBean.id);
        if (updated) {
          setSelectedBean(updated);
        } else {
          setSelectedBean(null);
          setRecentRecords([]);
        }
      }
    } catch (err) {
      setError('加载数据失败，请确认后端服务是否启动');
    } finally {
      setLoading(false);
    }
  }, [filterRoast, search, sortBy, sortDir, selectedBean]);

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

  const handleEditBean = async (id: string, req: any) => {
    try {
      await beanService.updateBean(id, req);
      loadBeans();
    } catch (err) {
      setError('编辑咖啡豆失败');
    }
  };

  const handleRestock = async (id: string, req: any) => {
    try {
      await beanService.restockBean(id, req);
      loadBeans();
      if (selectedBean?.id === id) {
        const records = await beanService.getRecentRecords(id, 5);
        setRecentRecords(records);
      }
    } catch (err: any) {
      setError(err.message || '补货失败');
    }
  };

  const handleConsume = async (id: string, req: any) => {
    try {
      await beanService.consumeBean(id, req);
      loadBeans();
      if (selectedBean?.id === id) {
        const records = await beanService.getRecentRecords(id, 5);
        setRecentRecords(records);
      }
    } catch (err: any) {
      setError(err.message || '消耗记录失败');
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('确定要删除这款咖啡豆吗？删除后不可恢复。')) return;
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

      <LowStockBanner summary={warningSummary} />

      {error && (
        <div className="error-message" onClick={() => setError('')}>
          {error}（点击关闭） ×
        </div>
      )}

      <StatisticsPanel />

      <div className="main-content">
        <div className="left-panel">
          <SearchSortBar
            search={search}
            onSearchChange={setSearch}
            filterRoast={filterRoast}
            onFilterRoastChange={setFilterRoast}
            sortBy={sortBy}
            onSortByChange={setSortBy}
            sortDir={sortDir}
            onSortDirChange={setSortDir}
            beanCount={beans.length}
          />

          {loading ? (
            <div className="loading">加载中...</div>
          ) : (
            <BeanTable
              beans={beans}
              selectedBeanId={selectedBean?.id || null}
              onSelectBean={handleSelectBean}
              onEdit={setEditBean}
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
          <ImportExportPanel onRefresh={loadBeans} />
        </div>
      </div>

      <EditBeanDialog
        bean={editBean}
        onClose={() => setEditBean(null)}
        onSave={handleEditBean}
      />
    </div>
  );
};

export default InventoryView;
