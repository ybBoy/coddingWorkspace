export type RoomStatus = 'VACANT' | 'OCCUPIED' | 'DIRTY' | 'MAINTENANCE' | 'DISABLED';

export type PageType = 'status' | 'admin';

export type AlertType = 'overdue' | 'maintenance' | 'dirty';

export interface Room {
  id: string;
  roomNo: string;
  floor: number;
  status: RoomStatus;
  type: string;
  currentStay?: StayRecord | null;
  isOverdue: boolean;
  defaultPrice: number;
}

export interface StayRecord {
  id: string;
  roomId: string;
  guestName: string;
  checkInTime: number;
  expectedCheckOutTime: number;
  actualCheckOutTime?: number | null;
  price: number;
  deposit: number;
  settled: boolean;
  checkInOperator: string;
  checkOutOperator?: string | null;
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

export interface Operator {
  id: string;
  username: string;
  name: string;
  role: string;
}

export interface AlertItem {
  id: string;
  type: AlertType;
  roomId: string;
  roomNo: string;
  message: string;
  timestamp: number;
  triggerTime: number;
}

export interface RoomDetail {
  room: Room;
  stayHistory: StayRecord[];
  logs: RoomLog[];
}

export type WsMessageType =
  | 'ROOMS_UPDATE'
  | 'LOGS_UPDATE'
  | 'FULL_SYNC'
  | 'ALERTS_UPDATE'
  | 'ROOM_DETAIL'
  | 'OPERATORS_LIST'
  | 'EXPORT_DATA';

export interface WsMessage {
  type: WsMessageType;
  payload: any;
}

export interface FullSyncPayload {
  rooms: Room[];
  logs: RoomLog[];
  alerts: AlertItem[];
  operators: Operator[];
  currentOperator: string;
}

export const RoomStatusText: Record<RoomStatus, string> = {
  VACANT: '空房',
  OCCUPIED: '已入住',
  DIRTY: '待打扫',
  MAINTENANCE: '维修中',
  DISABLED: '已停用',
};

export const AlertTypeText: Record<AlertType, string> = {
  overdue: '离店超时',
  maintenance: '维修超时',
  dirty: '待打扫积压',
};
