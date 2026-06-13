export interface Plant {
  id: string;
  name: string;
  location: string;
  lightRequirement: string;
  status: string;
  wateringIntervalDays: number;
  lastWateredTime: string | null;
  createdAt: string;
  needsWatering?: boolean;
}

export interface CareLog {
  id: string;
  type: string;
  note: string;
  timestamp: string;
}

export type CareType = 'WATERING' | 'FERTILIZING' | 'PRUNING';

export interface CreatePlantRequest {
  name: string;
  location: string;
  lightRequirement: string;
  status: string;
  wateringIntervalDays: number;
}

export interface AddCareLogRequest {
  type: string;
  note: string;
}

export interface UpdateStatusRequest {
  status: string;
}
