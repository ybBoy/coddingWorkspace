/**
 * 前端类型定义文件
 * 职责：定义所有业务数据的 TypeScript 类型，包括号票、窗口、队列状态、WebSocket 消息等
 */

export type BusinessType = '咨询' | '办理' | '售后';

export type TicketStatus = 'waiting' | 'calling' | 'completed' | 'missed';

export interface Ticket {
  id: string;
  number: number;
  businessType: BusinessType;
  status: TicketStatus;
  createdAt: number;
  calledAt?: number;
  counterId?: string;
}

export type CounterStatus = 'idle' | 'busy';

export interface Counter {
  id: string;
  name: string;
  status: CounterStatus;
  currentTicket?: Ticket;
}

export interface CallRecord {
  ticket: Ticket;
  counterName: string;
  action: 'called' | 'completed' | 'missed' | 'recalled';
  timestamp: number;
}

export interface QueueState {
  waitingQueue: Ticket[];
  counters: Counter[];
  currentCalling: Ticket | null;
  callRecords: CallRecord[];
  nextNumber: number;
}

export type WsAction =
  | 'GET_STATE'
  | 'TAKE_TICKET'
  | 'CALL_NEXT'
  | 'COMPLETE'
  | 'MISS'
  | 'RECALL'
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
