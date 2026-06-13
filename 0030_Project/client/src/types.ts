export type PetStatus = 'NORMAL' | 'NEED_ATTENTION' | 'PICKED_UP';

export type UserRole = 'ADMIN' | 'STAFF';

export interface Pet {
  id: string;
  name: string;
  breed: string;
  ownerPhoneLast4: string;
  status: PetStatus;
  checkInTime: string;
}

export interface CareRecord {
  id: string;
  petId: string;
  petName: string;
  action: string;
  note: string;
  staffName: string;
  time: string;
}

export interface StatusChange {
  id: string;
  petId: string;
  petName: string;
  oldStatus: PetStatus;
  newStatus: PetStatus;
  staffName: string;
  time: string;
}

export interface ReminderConfig {
  action: string;
  intervalMinutes: number;
  enabled: boolean;
}

export interface ShiftSummary {
  attentionPets: Pet[];
  pickedUpPets: Pet[];
  todayRecordCount: number;
  todayRecords: CareRecord[];
  totalPetsInStore: number;
}

export interface CareAction {
  label: string;
  value: string;
  emoji: string;
  defaultIntervalMinutes: number;
}

export const CARE_ACTIONS: CareAction[] = [
  { label: '喂食', value: 'FEED', emoji: '🍖', defaultIntervalMinutes: 360 },
  { label: '遛弯', value: 'WALK', emoji: '🐾', defaultIntervalMinutes: 360 },
  { label: '洗澡', value: 'BATH', emoji: '🛁', defaultIntervalMinutes: 720 },
  { label: '梳毛', value: 'GROOM', emoji: '✂️', defaultIntervalMinutes: 0 },
  { label: '玩耍', value: 'PLAY', emoji: '🎾', defaultIntervalMinutes: 0 },
  { label: '喂药', value: 'MEDICINE', emoji: '💊', defaultIntervalMinutes: 0 },
];

export const REMINDABLE_ACTIONS = CARE_ACTIONS.filter((a) => a.defaultIntervalMinutes > 0);

export const STATUS_LABELS: Record<PetStatus, string> = {
  NORMAL: '正常',
  NEED_ATTENTION: '需要关注',
  PICKED_UP: '已接走',
};
