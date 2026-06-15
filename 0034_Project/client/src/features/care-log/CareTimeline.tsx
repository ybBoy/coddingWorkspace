import React, { useState, useEffect } from 'react';
import { CareLog, CARE_TYPE_OPTIONS } from '../../types';
import { plantApi } from '../../api/plantApi';
import styles from '../../styles/careTimeline.module.css';

interface CareTimelineProps {
  plantId: string;
  plantName: string;
}

const CareTimeline: React.FC<CareTimelineProps> = ({ plantId, plantName }) => {
  const [timeline, setTimeline] = useState<Record<string, CareLog[]>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (plantId) {
      loadTimeline();
    }
  }, [plantId]);

  const loadTimeline = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await plantApi.getCareTimeline(plantId);
      setTimeline(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  const getCareTypeInfo = (type: string) => {
    return CARE_TYPE_OPTIONS.find((t) => t.value === type) || CARE_TYPE_OPTIONS[0];
  };

  const formatTime = (timestamp: string) => {
    try {
      if (timestamp.includes('T')) {
        const [, timePart] = timestamp.split('T');
        const timeMain = timePart.split('.')[0];
        const [hour, minute] = timeMain.split(':');
        return `${hour}:${minute}`;
      }
      const date = new Date(timestamp);
      if (!isNaN(date.getTime())) {
        return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      }
      return '';
    } catch (e) {
      return '';
    }
  };

  const formatDateDisplay = (dateStr: string) => {
    try {
      const [year, month, day] = dateStr.split('-');
      const date = new Date(parseInt(year), parseInt(month) - 1, parseInt(day));
      const today = new Date();
      const yesterday = new Date(today);
      yesterday.setDate(yesterday.getDate() - 1);

      if (
        date.getFullYear() === today.getFullYear() &&
        date.getMonth() === today.getMonth() &&
        date.getDate() === today.getDate()
      ) {
        return '今天';
      }
      if (
        date.getFullYear() === yesterday.getFullYear() &&
        date.getMonth() === yesterday.getMonth() &&
        date.getDate() === yesterday.getDate()
      ) {
        return '昨天';
      }

      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
      const weekday = weekdays[date.getDay()];
      return `${month}月${day}日 ${weekday}`;
    } catch (e) {
      return dateStr;
    }
  };

  const sortedDates = Object.keys(timeline).sort((a, b) => b.localeCompare(a));

  if (loading) {
    return (
      <div className={styles.loading}>
        <span className={styles.loadingSpinner}>⏳</span>
        <span>加载养护时间线...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.error}>
        <span>❌ {error}</span>
        <button className={styles.retryBtn} onClick={loadTimeline}>
          重试
        </button>
      </div>
    );
  }

  if (sortedDates.length === 0) {
    return (
      <div className={styles.empty}>
        <div className={styles.emptyIcon}>📅</div>
        <p>暂无养护记录</p>
        <p className={styles.emptyHint}>记录第一次浇水、施肥或修剪吧</p>
      </div>
    );
  }

  return (
    <div className={styles.timelineContainer}>
      <div className={styles.timelineHeader}>
        <h3 className={styles.timelineTitle}>📅 {plantName} - 养护时间线</h3>
      </div>
      <div className={styles.timeline}>
        {sortedDates.map((date) => {
          const logs = timeline[date];
          return (
            <div key={date} className={styles.dateGroup}>
              <div className={styles.dateHeader}>
                <span className={styles.dateBadge}>{formatDateDisplay(date)}</span>
                <span className={styles.dateCountBadge}>{logs.length} 条记录</span>
              </div>
              <div className={styles.logsContainer}>
                {logs.map((log, index) => {
                  const typeInfo = getCareTypeInfo(log.type);
                  return (
                    <div key={log.id} className={styles.logItem}>
                      <div className={styles.logLine} />
                      <div className={styles.logDot}>
                        <span className={styles.logIcon}>{typeInfo.icon}</span>
                      </div>
                      <div className={styles.logContent}>
                        <div className={styles.logTime}>{formatTime(log.timestamp)}</div>
                        <div className={styles.logType} style={{ color: typeInfo.value === 'WATERING' ? '#2196F3' : typeInfo.value === 'FERTILIZING' ? '#4CAF50' : '#FF9800' }}>
                          {typeInfo.label}
                        </div>
                        {log.note && <div className={styles.logNote}>{log.note}</div>}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default CareTimeline;
