export type PetStatus = 'NORMAL' | 'NEED_ATTENTION' | 'PICKED_UP';

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
  time: string;
}

export type EventType =
  | 'petAdded'
  | 'statusUpdated'
  | 'careRecordAdded'
  | 'initData'
  | 'filterChanged'
  | 'socketConnected'
  | 'socketDisconnected';

export interface CareAction {
  label: string;
  value: string;
  emoji: string;
}

export const CARE_ACTIONS: CareAction[] = [
  { label: '喂食', value: 'FEED', emoji: '🍖' },
  { label: '遛弯', value: 'WALK', emoji: '🐾' },
  { label: '洗澡', value: 'BATH', emoji: '🛁' },
  { label: '梳毛', value: 'GROOM', emoji: '✂️' },
  { label: '玩耍', value: 'PLAY', emoji: '🎾' },
  { label: '喂药', value: 'MEDICINE', emoji: '💊' },
];
