export type RoomStatus = 'VACANT' | 'OCCUPIED' | 'DIRTY' | 'MAINTENANCE';

export interface Room {
  id: string;
  roomNo: string;
  floor: number;
  status: RoomStatus;
  type: string;
  currentStay?: StayRecord | null;
  isOverdue: boolean;
}

export interface StayRecord {
  id: string;
  roomId: string;
  guestName: string;
  checkInTime: number;
  expectedCheckOutTime: number;
  actualCheckOutTime?: number | null;
}

export interface RoomLog {
  id: string;
  roomId: string;
  roomNo: string;
  action: string;
  operator: string;
  timestamp: number;
  remark?: string;
}

export interface WsMessage {
  type: 'ROOMS_UPDATE' | 'LOGS_UPDATE' | 'FULL_SYNC';
  payload: any;
}

export const RoomStatusText: Record<RoomStatus, string> = {
  VACANT: '空房',
  OCCUPIED: '已入住',
  DIRTY: '待打扫',
  MAINTENANCE: '维修中',
};
