export type PlantStatus = 'HEALTHY' | 'GROWING_WELL' | 'NEEDS_ATTENTION' | 'SICK' | 'DORMANT';

export const PLANT_STATUS_OPTIONS: Array<{ value: PlantStatus; label: string; color: string }> = [
  { value: 'HEALTHY', label: '健康', color: '#2d6a4f' },
  { value: 'GROWING_WELL', label: '生长良好', color: '#40916c' },
  { value: 'NEEDS_ATTENTION', label: '需要关注', color: '#f4a261' },
  { value: 'SICK', label: '生病', color: '#e76f51' },
  { value: 'DORMANT', label: '休眠', color: '#6c757d' },
];

export type CareType = 'WATERING' | 'FERTILIZING' | 'PRUNING';

export const CARE_TYPE_OPTIONS: Array<{ value: CareType; label: string; icon: string }> = [
  { value: 'WATERING', label: '浇水', icon: '💧' },
  { value: 'FERTILIZING', label: '施肥', icon: '🌱' },
  { value: 'PRUNING', label: '修剪', icon: '✂️' },
];

export interface Plant {
  id: string;
  name: string;
  location: string;
  lightRequirement: string;
  status: string;
  wateringIntervalDays: number;
  lastWateredTime: string | null;
  nextWateringTime?: string;
  createdAt: string;
  photoUrl?: string;
  careLogs: CareLog[];
  needsWatering?: boolean;
}

export interface CareLog {
  id: string;
  type: CareType;
  note: string;
  timestamp: string;
}

export interface PlantStatistics {
  totalPlants: number;
  needingWaterCount: number;
  wateringCountThisWeek: number;
  fertilizingCountThisWeek: number;
  pruningCountThisWeek: number;
  longNeglectedCount: number;
  plantsByLocation: Record<string, number>;
  plantsByStatus: Record<string, number>;
}

export interface CreatePlantRequest {
  name: string;
  location: string;
  lightRequirement: string;
  status: string;
  wateringIntervalDays: number;
  photoUrl?: string;
}

export interface AddCareLogRequest {
  type: CareType;
  note: string;
}

export interface UpdateStatusRequest {
  status: string;
}

export interface UpdatePhotoRequest {
  photoUrl: string;
}

export interface TimelineGroup {
  date: string;
  logs: CareLog[];
}
