import React from 'react';
import { PetStatus } from '../types';
import { eventBus } from '../core/EventBus';

export type FilterValue = 'ALL' | PetStatus;

interface StatusFilterProps {
  currentFilter: FilterValue;
}

const filters: { value: FilterValue; label: string; emoji: string }[] = [
  { value: 'ALL', label: '全部', emoji: '🐾' },
  { value: 'NORMAL', label: '正常', emoji: '✅' },
  { value: 'NEED_ATTENTION', label: '需要关注', emoji: '⚠️' },
  { value: 'PICKED_UP', label: '已接走', emoji: '👋' },
];

const StatusFilter: React.FC<StatusFilterProps> = ({ currentFilter }) => {
  const handleFilterChange = (value: FilterValue) => {
    eventBus.emit('filterChanged', value);
  };

  return (
    <div className="status-filter">
      <div className="filter-label">按状态筛选：</div>
      <div className="filter-buttons">
        {filters.map((filter) => (
          <button
            key={filter.value}
            className={`filter-btn ${currentFilter === filter.value ? 'active' : ''} filter-${filter.value.toLowerCase()}`}
            onClick={() => handleFilterChange(filter.value)}
          >
            <span className="filter-emoji">{filter.emoji}</span>
            <span>{filter.label}</span>
          </button>
        ))}
      </div>
    </div>
  );
};

export default StatusFilter;
