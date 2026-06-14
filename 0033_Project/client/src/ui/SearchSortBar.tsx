import React from 'react';
import { FILTER_OPTIONS } from '../constants/roastLevels';
import { SortField, SortDir } from '../types';

interface SearchSortBarProps {
  search: string;
  onSearchChange: (value: string) => void;
  filterRoast: string;
  onFilterRoastChange: (value: string) => void;
  sortBy: SortField;
  onSortByChange: (value: SortField) => void;
  sortDir: SortDir;
  onSortDirChange: (value: SortDir) => void;
  beanCount: number;
}

const SORT_OPTIONS: { value: SortField; label: string }[] = [
  { value: 'name', label: '豆子名称' },
  { value: 'origin', label: '产地' },
  { value: 'stock', label: '当前库存' },
  { value: 'minStock', label: '最低库存线' },
  { value: 'lastModified', label: '最近变动时间' },
];

const SearchSortBar: React.FC<SearchSortBarProps> = ({
  search,
  onSearchChange,
  filterRoast,
  onFilterRoastChange,
  sortBy,
  onSortByChange,
  sortDir,
  onSortDirChange,
  beanCount,
}) => {
  return (
    <div className="search-sort-bar">
      <div className="search-row">
        <div className="search-box">
          <span className="search-icon">🔍</span>
          <input
            type="text"
            placeholder="搜索豆子名称或产地..."
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
            className="search-input"
          />
        </div>
        <div className="sort-controls">
          <label>排序：</label>
          <select
            value={sortBy}
            onChange={(e) => onSortByChange(e.target.value as SortField)}
          >
            {SORT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <button
            className={`sort-dir-btn ${sortDir}`}
            onClick={() => onSortDirChange(sortDir === 'asc' ? 'desc' : 'asc')}
            title={sortDir === 'asc' ? '升序' : '降序'}
          >
            {sortDir === 'asc' ? '↑ 升序' : '↓ 降序'}
          </button>
        </div>
      </div>
      <div className="filter-row">
        <div className="filter-group">
          <label>烘焙程度：</label>
          <div className="filter-tags">
            {FILTER_OPTIONS.map((option) => {
              const isActive =
                option.code === '' ? !filterRoast : filterRoast === option.code;
              return (
                <button
                  key={option.code || 'all'}
                  className={`filter-tag ${isActive ? 'active' : ''}`}
                  onClick={() => onFilterRoastChange(option.code)}
                >
                  {option.label}
                </button>
              );
            })}
          </div>
        </div>
        <div className="bean-count">
          共 <strong>{beanCount}</strong> 款咖啡豆
        </div>
      </div>
    </div>
  );
};

export default SearchSortBar;
