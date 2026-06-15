import React, { useState, useEffect } from 'react';
import { PlantStatistics, PLANT_STATUS_OPTIONS } from '../../types';
import { plantApi } from '../../api/plantApi';
import styles from '../../styles/statisticsPanel.module.css';

interface StatisticsPanelProps {
  onClose?: () => void;
}

const StatisticsPanel: React.FC<StatisticsPanelProps> = ({ onClose }) => {
  const [statistics, setStatistics] = useState<PlantStatistics | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadStatistics();
  }, []);

  const loadStatistics = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await plantApi.getStatistics();
      setStatistics(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  const getStatusLabel = (statusKey: string) => {
    return PLANT_STATUS_OPTIONS.find((s) => s.value === statusKey)?.label || statusKey;
  };

  const getStatusColor = (statusKey: string) => {
    return PLANT_STATUS_OPTIONS.find((s) => s.value === statusKey)?.color || '#6c757d';
  };

  if (loading) {
    return (
      <div className={styles.loading}>
        <span className={styles.loadingSpinner}>⏳</span>
        <span>加载统计数据...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.error}>
        <span>❌ {error}</span>
        <button className={styles.retryBtn} onClick={loadStatistics}>
          重试
        </button>
      </div>
    );
  }

  if (!statistics) {
    return null;
  }

  return (
    <div className={styles.panel}>
      <div className={styles.panelHeader}>
        <h3 className={styles.panelTitle}>📊 养护统计</h3>
        {onClose && (
          <button className={styles.closeBtn} onClick={onClose}>
            ✕
          </button>
        )}
      </div>

      <div className={styles.statsGrid}>
        <div className={styles.statCard}>
          <div className={styles.statIcon}>🌿</div>
          <div className={styles.statContent}>
            <div className={styles.statValue}>{statistics.totalPlants}</div>
            <div className={styles.statLabel}>总植物数</div>
          </div>
        </div>

        <div className={styles.statCard} style={{ borderLeftColor: '#e76f51' }}>
          <div className={styles.statIcon}>💧</div>
          <div className={styles.statContent}>
            <div className={styles.statValue} style={{ color: '#e76f51' }}>{statistics.needingWaterCount}</div>
            <div className={styles.statLabel}>需要浇水</div>
          </div>
        </div>

        <div className={styles.statCard} style={{ borderLeftColor: '#2196F3' }}>
          <div className={styles.statIcon}>💧</div>
          <div className={styles.statContent}>
            <div className={styles.statValue} style={{ color: '#2196F3' }}>{statistics.wateringCountThisWeek}</div>
            <div className={styles.statLabel}>本周浇水</div>
          </div>
        </div>

        <div className={styles.statCard} style={{ borderLeftColor: '#4CAF50' }}>
          <div className={styles.statIcon}>🌱</div>
          <div className={styles.statContent}>
            <div className={styles.statValue} style={{ color: '#4CAF50' }}>{statistics.fertilizingCountThisWeek}</div>
            <div className={styles.statLabel}>本周施肥</div>
          </div>
        </div>

        <div className={styles.statCard} style={{ borderLeftColor: '#FF9800' }}>
          <div className={styles.statIcon}>✂️</div>
          <div className={styles.statContent}>
            <div className={styles.statValue} style={{ color: '#FF9800' }}>{statistics.pruningCountThisWeek}</div>
            <div className={styles.statLabel}>本周修剪</div>
          </div>
        </div>

        <div className={styles.statCard} style={{ borderLeftColor: '#9C27B0' }}>
          <div className={styles.statIcon}>⚠️</div>
          <div className={styles.statContent}>
            <div className={styles.statValue} style={{ color: '#9C27B0' }}>{statistics.longNeglectedCount}</div>
            <div className={styles.statLabel}>长期未养护</div>
          </div>
        </div>
      </div>

      {Object.keys(statistics.plantsByLocation).length > 0 && (
        <div className={styles.section}>
          <h4 className={styles.sectionTitle}>📍 按位置分布</h4>
          <div className={styles.barChart}>
            {Object.entries(statistics.plantsByLocation)
              .sort((a, b) => b[1] - a[1])
              .map(([location, count]) => (
                <div key={location} className={styles.barItem}>
                  <div className={styles.barLabel}>
                    <span>{location}</span>
                    <span className={styles.barCount}>{count} 盆</span>
                  </div>
                  <div className={styles.barTrack}>
                    <div
                      className={styles.barFill}
                      style={{
                        width: `${(count / Math.max(...Object.values(statistics.plantsByLocation))) * 100}%`,
                        backgroundColor: '#40916c',
                      }}
                    />
                  </div>
                </div>
              ))}
          </div>
        </div>
      )}

      {Object.keys(statistics.plantsByStatus).length > 0 && (
        <div className={styles.section}>
          <h4 className={styles.sectionTitle}>🏷️ 按状态分布</h4>
          <div className={styles.statusGrid}>
            {Object.entries(statistics.plantsByStatus).map(([status, count]) => (
              <div
                key={status}
                className={styles.statusCard}
                style={{
                  backgroundColor: getStatusColor(status) + '15',
                  borderColor: getStatusColor(status),
                }}
              >
                <div className={styles.statusCount} style={{ color: getStatusColor(status) }}>
                  {count}
                </div>
                <div className={styles.statusLabel}>{getStatusLabel(status)}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default StatisticsPanel;
