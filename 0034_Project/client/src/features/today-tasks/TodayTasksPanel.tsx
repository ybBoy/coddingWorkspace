import React, { useState, useEffect } from 'react';
import { TaskItem } from '../../types';
import { plantApi } from '../../api/plantApi';
import styles from '../../styles/todayTasks.module.css';

interface TodayTasksPanelProps {
  onLocatePlant: (plantId: string) => void;
}

const TodayTasksPanel: React.FC<TodayTasksPanelProps> = ({ onLocatePlant }) => {
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const loadTasks = async () => {
    try {
      setLoading(true);
      const data = await plantApi.getTodayTasks();
      setTasks(data);
    } catch (err) {
      console.error('Error loading today tasks:', err);
      setTasks([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTasks();
  }, []);

  if (loading) {
    return (
      <div className={styles.panel}>
        <h3 className={styles.title}>📋 今日养护任务</h3>
        <p className={styles.loadingText}>加载中...</p>
      </div>
    );
  }

  if (tasks.length === 0) {
    return (
      <div className={styles.panel}>
        <h3 className={styles.title}>📋 今日养护任务</h3>
        <div className={styles.emptyState}>
          <div className={styles.emptyIcon}>🎉</div>
          <p className={styles.emptyText}>今天没有待处理任务</p>
          <p className={styles.emptySubText}>所有植物都照顾得很好！</p>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.panel}>
      <h3 className={styles.title}>
        📋 今日养护任务
        <span className={styles.taskCount}>{tasks.length} 项</span>
      </h3>
      <div className={styles.taskList}>
        {tasks.map((task, idx) => (
          <div
            key={`${task.plantId}-${task.taskType}-${idx}`}
            className={`${styles.taskItem} ${styles[`taskType_${task.taskType}`]}`}
            onClick={() => onLocatePlant(task.plantId)}
          >
            <span className={styles.taskIcon}>{task.icon}</span>
            <div className={styles.taskContent}>
              <div className={styles.taskPlantName}>{task.plantName}</div>
              <div className={styles.taskReason}>{task.reason}</div>
            </div>
            <span className={styles.taskArrow}>→</span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default TodayTasksPanel;
