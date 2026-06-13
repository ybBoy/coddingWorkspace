import React from 'react';
import { ZoneType, ZONE_LABELS } from '../core/EventBus';

interface ZoneFilterProps {
  activeZone: ZoneType | 'all';
  onChange: (zone: ZoneType | 'all') => void;
}

const ZONES: (ZoneType | 'all')[] = ['all', 'window', 'computer', 'quiet', 'standard'];
const ZONE_ALL_LABEL = '全部区域';

const ZoneFilter: React.FC<ZoneFilterProps> = ({ activeZone, onChange }) => {
  return (
    <div className="zone-filter">
      <span className="zone-filter-label">区域：</span>
      <div className="zone-filter-buttons">
        {ZONES.map((z) => (
          <button
            key={z}
            className={`zone-btn ${activeZone === z ? 'active' : ''} ${z !== 'all' ? `zone-${z}` : ''}`}
            onClick={() => onChange(z)}
          >
            {z === 'all' ? ZONE_ALL_LABEL : ZONE_LABELS[z as ZoneType]}
          </button>
        ))}
      </div>
    </div>
  );
};

export default ZoneFilter;
