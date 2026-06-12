/**
 * TodayStats 今日统计面板
 * 职责：展示今日取号总数、等待、办理中、完成、过号、平均等待时长
 * 通过 WebSocket 推送实时更新
 */
import React, { useEffect, useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { QueueState, TodayStats as StatsType } from './types';

const formatDuration = (seconds: number): string => {
  if (!seconds || seconds < 0) return '0 分';
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  if (mins === 0) return `${secs} 秒`;
  if (secs === 0) return `${mins} 分`;
  return `${mins} 分 ${secs} 秒`;
};

const TodayStats: React.FC = () => {
  const [stats, setStats] = useState<StatsType>({
    totalTaken: 0,
    waiting: 0,
    inProgress: 0,
    completed: 0,
    missed: 0,
    avgWaitSeconds: 0,
  });

  useEffect(() => {
    const unsub = eventBus.on(EVENTS.QUEUE_STATE_UPDATED, (state: QueueState) => {
      if (state.todayStats) {
        setStats(state.todayStats);
      }
    });
    return unsub;
  }, []);

  const cards = [
    { label: '今日取号', value: stats.totalTaken, className: 'stat-blue' },
    { label: '等待中', value: stats.waiting, className: 'stat-gray' },
    { label: '办理中', value: stats.inProgress, className: 'stat-cyan' },
    { label: '已完成', value: stats.completed, className: 'stat-green' },
    { label: '已过号', value: stats.missed, className: 'stat-orange' },
    { label: '平均等待', value: formatDuration(stats.avgWaitSeconds), className: 'stat-purple' },
  ];

  return (
    <section className="today-stats">
      <h2 className="section-title">今日统计</h2>
      <div className="stats-grid">
        {cards.map((card) => (
          <div key={card.label} className={`stat-card ${card.className}`}>
            <div className="stat-value">{card.value}</div>
            <div className="stat-label">{card.label}</div>
          </div>
        ))}
      </div>
    </section>
  );
};

export default TodayStats;
