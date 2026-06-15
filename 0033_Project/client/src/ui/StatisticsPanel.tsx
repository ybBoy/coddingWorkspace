import React, { useEffect, useState } from 'react';
import { StatisticsResponse } from '../types';
import { beanService } from '../services/beanService';
import { getRoastLabel } from '../constants/roastLevels';

interface StatisticsPanelProps {
  initialData?: StatisticsResponse | null;
  onRefresh?: () => Promise<void>;
}

const StatisticsPanel: React.FC<StatisticsPanelProps> = ({ initialData, onRefresh }) => {
  const [stats, setStats] = useState<StatisticsResponse | null>(initialData || null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (initialData !== undefined) {
      setStats(initialData);
    }
  }, [initialData]);

  const loadStats = async () => {
    try {
      setLoading(true);
      if (onRefresh) {
        await onRefresh();
      } else {
        const data = await beanService.getStatistics();
        setStats(data);
      }
    } catch (err) {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!initialData) {
      loadStats();
    }
    const timer = setInterval(() => {
      if (onRefresh) {
        onRefresh();
      } else {
        loadStats();
      }
    }, 30000);
    return () => clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loading && !stats) {
    return <div className="stats-panel loading-panel">加载中...</div>;
  }

  if (!stats) {
    return (
      <div className="stats-panel">
        <div className="panel-header">
          <h3>📊 数据统计</h3>
          <button className="btn btn-small btn-secondary" onClick={loadStats}>
            刷新
          </button>
        </div>
        <div className="empty-hint">暂无数据</div>
      </div>
    );
  }

  return (
    <div className="stats-panel">
      <div className="panel-header">
        <h3>📊 数据统计</h3>
        <button className="btn btn-small btn-secondary" onClick={loadStats}>
          刷新
        </button>
      </div>

      <div className="stats-grid">
        <div className="stat-card card-total">
          <div className="stat-value">{stats.totalStockGrams.toLocaleString()}</div>
          <div className="stat-label">总库存 (克)</div>
        </div>
        <div className="stat-card card-kinds">
          <div className="stat-value">{stats.totalBeanKinds}</div>
          <div className="stat-label">豆种数量 (款)</div>
        </div>
      </div>

      <div className="warning-breakdown">
        <h4>库存预警分布</h4>
        <div className="warning-bars">
          <div className="warning-bar-row">
            <span className="bar-label">已耗尽</span>
            <div className="bar-wrapper">
              <div
                className="bar-fill bar-empty"
                style={{ width: `${stats.totalBeanKinds ? (stats.emptyCount / stats.totalBeanKinds) * 100 : 0}%` }}
              />
            </div>
            <span className="bar-count count-empty">{stats.emptyCount}</span>
          </div>
          <div className="warning-bar-row">
            <span className="bar-label">已不足</span>
            <div className="bar-wrapper">
              <div
                className="bar-fill bar-low"
                style={{ width: `${stats.totalBeanKinds ? (stats.lowStockCount / stats.totalBeanKinds) * 100 : 0}%` }}
              />
            </div>
            <span className="bar-count count-low">{stats.lowStockCount}</span>
          </div>
          <div className="warning-bar-row">
            <span className="bar-label">即将不足</span>
            <div className="bar-wrapper">
              <div
                className="bar-fill bar-approaching"
                style={{ width: `${stats.totalBeanKinds ? (stats.approachingCount / stats.totalBeanKinds) * 100 : 0}%` }}
              />
            </div>
            <span className="bar-count count-approaching">{stats.approachingCount}</span>
          </div>
        </div>
      </div>

      {stats.weeklyTopConsumed.length > 0 && (
        <div className="top-consumed">
          <h4>本周消耗 TOP 5</h4>
          <ul className="top-list">
            {stats.weeklyTopConsumed.map((item, idx) => (
              <li key={item.beanId} className="top-item">
                <span className={`rank rank-${idx + 1}`}>{idx + 1}</span>
                <span className="top-name">{item.beanName}</span>
                <span className="top-amount">{item.totalConsumed}g</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {stats.lowStockBeans.length > 0 && (
        <div className="low-list-section">
          <h4>低库存 / 耗尽豆子</h4>
          <ul className="low-bean-list">
            {(stats.lowStockBeans as any[]).slice(0, 5).map((b) => {
              const isEmpty = b.stockGrams <= 0;
              return (
                <li key={b.id} className={`low-bean-item ${isEmpty ? 'empty' : 'low'}`}>
                  <span className="low-bean-name">{b.name}</span>
                  <span className={`low-bean-stock ${isEmpty ? 'empty' : ''}`}>
                    {isEmpty ? '已耗尽' : `${b.stockGrams}g`}
                  </span>
                  <span className="low-bean-roast">{getRoastLabel(b.roastLevel)}</span>
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
};

export default StatisticsPanel;
