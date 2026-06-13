import type { ReadingStatus } from '../types/book';
import { STATUS_LABELS } from '../types/book';

export type FilterValue = ReadingStatus | 'ALL';

interface StatusFilterProps {
  value: FilterValue;
  onChange: (value: FilterValue) => void;
}

const FILTERS: { key: FilterValue; label: string }[] = [
  { key: 'ALL', label: '全部' },
  { key: 'TO_READ', label: STATUS_LABELS.TO_READ },
  { key: 'READING', label: STATUS_LABELS.READING },
  { key: 'READ', label: STATUS_LABELS.READ }
];

export default function StatusFilter({ value, onChange }: StatusFilterProps) {
  return (
    <div className="status-filter">
      {FILTERS.map((f) => (
        <button
          key={f.key}
          className={`filter-btn ${value === f.key ? 'active' : ''}`}
          onClick={() => onChange(f.key)}
        >
          {f.label}
        </button>
      ))}
    </div>
  );
}
