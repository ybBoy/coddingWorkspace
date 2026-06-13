import React from 'react';
import { CareLog } from '../../types';
import styles from '../../styles/careLogPanel.module.css';

interface CareLogPanelProps {
  plantName: string;
  logs: CareLog[];
  onClose: () => void;
}

const CareLogPanel: React.FC<CareLogPanelProps> = ({ plantName, logs, onClose }) => {
  const getCareTypeLabel = (type: string) => {
    switch (type) {
      case 'WATERING':
        return { label: '浇水', icon: '💧', className: styles.typeWatering };
      case 'FERTILIZING':
        return { label: '施肥', icon: '🌱', className: styles.typeFertilizing };
      case 'PRUNING':
        return { label: '修剪', icon: '✂️', className: styles.typePruning };
      default:
        return { label: type, icon: '📝', className: styles.typeDefault };
    }
  };

  const formatDateTime = (dateStr: string) => {
    try {
      if (dateStr.includes('T')) {
        const [datePart, timePart] = dateStr.split('T');
        const [year, month, day] = datePart.split('-');
        const timeMain = timePart.split('.')[0];
        const [hour, minute] = timeMain.split(':');
        return `${year}-${month}-${day} ${hour}:${minute}`;
      }
      const date = new Date(dateStr);
      if (!isNaN(date.getTime())) {
        return date.toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
        });
      }
      return dateStr;
    } catch (e) {
      return dateStr;
    }
  };

  return (
    <div className={styles.panelContainer}>
      <div className={styles.panelHeader}>
        <div className={styles.panelTitle}>
          <span className={styles.titleIcon}>📋</span>
          <h3>「{plantName}」的养护记录</h3>
        </div>
        <button className={styles.closeBtn} onClick={onClose}>
          ×
        </button>
      </div>

      {logs.length === 0 ? (
        <div className={styles.emptyLogs}>
          <div className={styles.emptyIcon}>📭</div>
          <p>暂无养护记录</p>
          <p className={styles.emptyHint}>点击卡片上的按钮开始记录养护动作吧</p>
        </div>
      ) : (
        <div className={styles.logsList}>
          {logs.map((log, index) => {
            const typeInfo = getCareTypeLabel(log.type);
            return (
              <div key={log.id} className={styles.logItem}>
                <div className={styles.logIndex}>{index + 1}</div>
                <div className={`${styles.logType} ${typeInfo.className}`}>
                  <span className={styles.typeIcon}>{typeInfo.icon}</span>
                  <span>{typeInfo.label}</span>
                </div>
                <div className={styles.logContent}>
                  {log.note ? (
                    <p className={styles.logNote}>{log.note}</p>
                  ) : (
                    <p className={styles.logNoNote}>无备注</p>
                  )}
                  <p className={styles.logTime}>{formatDateTime(log.timestamp)}</p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default CareLogPanel;
