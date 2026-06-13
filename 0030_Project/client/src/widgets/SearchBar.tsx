import React, { useState } from 'react';
import { eventBus } from '../core/EventBus';

const SearchBar: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'time' | 'name'>('time');

  const handleSearch = (value: string) => {
    setSearchTerm(value);
    eventBus.emit('searchChanged', { searchTerm: value, sortBy });
  };

  const handleSort = (value: 'time' | 'name') => {
    setSortBy(value);
    eventBus.emit('searchChanged', { searchTerm, sortBy: value });
  };

  return (
    <div className="search-bar">
      <div className="search-input-wrap">
        <span className="search-icon">🔍</span>
        <input
          type="text"
          className="search-input"
          value={searchTerm}
          onChange={(e) => handleSearch(e.target.value)}
          placeholder="搜索宠物名字或品种..."
        />
        {searchTerm && (
          <button className="search-clear" onClick={() => handleSearch('')}>✕</button>
        )}
      </div>
      <div className="sort-buttons">
        <button className={`sort-btn ${sortBy === 'time' ? 'active' : ''}`} onClick={() => handleSort('time')}>按入住</button>
        <button className={`sort-btn ${sortBy === 'name' ? 'active' : ''}`} onClick={() => handleSort('name')}>按名字</button>
      </div>
    </div>
  );
};

export default SearchBar;
