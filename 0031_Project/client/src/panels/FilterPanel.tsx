import { useEffect, useState } from 'react';
import { eventBus, Events } from '../base/EventBus';
import { RoomStatusText, type RoomStatus } from '../base/types';
import './FilterPanel.css';

interface FilterPanelProps {
  floors: number[];
}

export function FilterPanel({ floors }: FilterPanelProps) {
  const [selectedFloor, setSelectedFloor] = useState<number | 'all'>('all');
  const [selectedStatus, setSelectedStatus] = useState<RoomStatus | 'all'>('all');

  useEffect(() => {
    eventBus.emit(Events.FILTER_CHANGED, {
      floor: selectedFloor,
      status: selectedStatus,
    });
  }, [selectedFloor, selectedStatus]);

  const handleFloorChange = (value: string) => {
    setSelectedFloor(value === 'all' ? 'all' : Number(value));
  };

  const handleStatusChange = (value: string) => {
    setSelectedStatus(value as RoomStatus | 'all');
  };

  return (
    <div className="filter-panel">
      <div className="filter-item">
        <label>楼层：</label>
        <select value={selectedFloor} onChange={(e) => handleFloorChange(e.target.value)}>
          <option value="all">全部楼层</option>
          {floors.map((floor) => (
            <option key={floor} value={floor}>
              {floor} 楼
            </option>
          ))}
        </select>
      </div>
      <div className="filter-item">
        <label>状态：</label>
        <select value={selectedStatus} onChange={(e) => handleStatusChange(e.target.value)}>
          <option value="all">全部状态</option>
          {(Object.keys(RoomStatusText) as RoomStatus[]).map((status) => (
            <option key={status} value={status}>
              {RoomStatusText[status]}
            </option>
          ))}
        </select>
      </div>
      <div className="filter-legend">
        <span className="legend-item">
          <span className="legend-dot status-vacant"></span>
          空房
        </span>
        <span className="legend-item">
          <span className="legend-dot status-occupied"></span>
          已入住
        </span>
        <span className="legend-item">
          <span className="legend-dot status-dirty"></span>
          待打扫
        </span>
        <span className="legend-item">
          <span className="legend-dot status-maintenance"></span>
          维修中
        </span>
      </div>
    </div>
  );
}
