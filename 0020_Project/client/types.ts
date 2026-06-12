/**
 * 前端类型定义文件
 * 职责：定义所有业务数据的 TypeScript 类型，包括号票、窗口、队列状态、WebSocket 消息等
 * 迭代新增：连接状态、Toast 反馈、过号列表、业务类型筛选、窗口配置、今日统计
 */

export type BusinessType = '咨询' | '办理' | '售后';

export const BUSINESS_TYPES: BusinessType[] = ['咨询', '办理', '售后'];

export type TicketStatus = 'waiting' | 'calling' | 'completed' | 'missed' | 'finished';

export interface Ticket {
  id: string;
  number: number;
  businessType: BusinessType;
  status: TicketStatus;
  createdAt: number;
  calledAt?: number;
  counterId?: string;
  completedAt?: number;
}

export type CounterStatus = 'idle' | 'busy';

export interface Counter {
  id: string;
  name: string;
  status: CounterStatus;
  enabled: boolean;
  supportedBusinessTypes: BusinessType[];
  currentTicket?: Ticket;
}

export interface CallRecord {
  ticket: Ticket;
  counterName: string;
  action: 'called' | 'completed' | 'missed' | 'recalled' | 'requeue' | 'finished';
  timestamp: number;
}

export interface TodayStats {
  totalTaken: number;
  waiting: number;
  inProgress: number;
  completed: number;
  missed: number;
  avgWaitSeconds: number;
}

export type ConnectionStatus = 'online' | 'reconnecting' | 'offline';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title: string;
  message?: string;
  duration?: number;
}

export interface QueueState {
  waitingQueue: Ticket[];
  missedQueue: Ticket[];
  counters: Counter[];
  currentCalling: Ticket | null;
  callRecords: CallRecord[];
  todayStats: TodayStats;
  nextNumber: number;
}

export type WsAction =
  | 'GET_STATE'
  | 'TAKE_TICKET'
  | 'CALL_NEXT'
  | 'CALL_NEXT_BY_TYPE'
  | 'COMPLETE'
  | 'MISS'
  | 'RECALL'
  | 'REQUEUE_MISSED'
  | 'FINISH_MISSED'
  | 'ADD_COUNTER'
  | 'UPDATE_COUNTER'
  | 'TOGGLE_COUNTER'
  | 'OPERATION_RESULT'
  | 'STATE_UPDATE';

export interface WsMessage {
  action: WsAction;
  payload?: any;
}

export interface TakeTicketPayload {
  businessType: BusinessType;
}

export interface CounterActionPayload {
  counterId: string;
  ticketId?: string;
}

export interface CallNextByTypePayload {
  counterId: string;
  businessType: BusinessType | 'all';
}

export interface MissedActionPayload {
  ticketId: string;
  counterId?: string;
}

export interface AddCounterPayload {
  name: string;
  supportedBusinessTypes: BusinessType[];
}

export interface UpdateCounterPayload {
  counterId: string;
  name?: string;
  supportedBusinessTypes?: BusinessType[];
}

export interface ToggleCounterPayload {
  counterId: string;
  enabled: boolean;
}

export interface OperationResultPayload {
  success: boolean;
  action: string;
  message: string;
}
