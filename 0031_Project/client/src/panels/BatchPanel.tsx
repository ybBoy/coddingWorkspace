import { useState } from 'react';
import { wsClient } from '../base/wsClient';
import type { Room } from '../base/types';

interface BatchPanelProps {
  rooms: Room[];
}

export function BatchPanel({ rooms }: BatchPanelProps) {
  const [selectedFloor, setSelectedFloor] = useState<number | 'all'>('all');

  const floors = Array.from(new Set(rooms.map((r) => r.floor))).sort((a, b) => a - b);

  const getFloorRooms = (floor: number) => rooms.filter((r) => r.floor === floor);
  const getDirtyCount = (floor: number) => getFloorRooms(floor).filter((r) => r.status === 'DIRTY').length;
  const getVacantCount = (floor: number) => getFloorRooms(floor).filter((r) => r.status === 'VACANT').length;

  const handleBatchClean = (floor: number) => {
    if (getDirtyCount(floor) === 0) {
      alert('该楼层没有待打扫的房间');
      return;
    }
    if (confirm(`确定将 ${floor} 楼所有待打扫房间（${getDirtyCount(floor)} 间）标记为已清洁？`)) {
      wsClient.batchCleanByFloor(floor);
    }
  };

  const handleBatchMarkDirty = (floor: number) => {
    if (getVacantCount(floor) === 0) {
      alert('该楼层没有空房');
      return;
    }
    if (confirm(`确定将 ${floor} 楼所有空房（${getVacantCount(floor)} 间）标记为待打扫？`)) {
      wsClient.batchMarkDirtyByFloor(floor);
    }
  };

  return (
    <div className="batch-panel">
      <h3 className="panel-title">批量操作</h3>
      <div className="floor-list">
        {floors.map((floor) => (
          <div key={floor} className="floor-batch-item">
            <div className="floor-batch-header">
              <span className="floor-name">{floor} 楼</span>
              <span className="floor-stats">
                共 {getFloorRooms(floor).length} 间
                {getDirtyCount(floor) > 0 && <span className="stat-dirty"> · 待打扫 {getDirtyCount(floor)}</span>}
                {getVacantCount(floor) > 0 && <span className="stat-vacant"> · 空房 {getVacantCount(floor)}</span>}
              </span>
            </div>
            <div className="batch-buttons">
              <button
                className="btn btn-secondary btn-sm"
                disabled={getDirtyCount(floor) === 0}
                onClick={() => handleBatchClean(floor)}
              >
                🧹 批量完成清洁
              </button>
              <button
                className="btn btn-warning btn-sm"
                disabled={getVacantCount(floor) === 0}
                onClick={() => handleBatchMarkDirty(floor)}
              >
                📋 批量设为待打扫
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
