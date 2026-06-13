export interface RoastOption {
  code: string;
  label: string;
}

export const ROAST_LEVELS: RoastOption[] = [
  { code: 'LIGHT', label: '浅烘焙' },
  { code: 'MEDIUM', label: '中烘焙' },
  { code: 'MEDIUM_DARK', label: '中深烘焙' },
  { code: 'DARK', label: '深烘焙' },
];

export const getRoastLabel = (code: string): string => {
  const option = ROAST_LEVELS.find((r) => r.code === code);
  return option ? option.label : code;
};

export const FILTER_OPTIONS: RoastOption[] = [
  { code: '', label: '全部' },
  ...ROAST_LEVELS,
];
