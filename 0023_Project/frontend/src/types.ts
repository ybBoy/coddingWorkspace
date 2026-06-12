export interface DanmakuMessage {
  id: string;
  content: string;
  nickname: string;
  timestamp: number;
  status: 'pending' | 'approved' | 'rejected';
  sensitive: boolean;
  color: string;
}

export interface AppState {
  sendingEnabled: boolean;
  pendingMessages: DanmakuMessage[];
  approvedMessages: DanmakuMessage[];
  mode: 'audience' | 'wall' | 'moderator';
  connected: boolean;
}

export type EventType =
  | 'SEND_MESSAGE'
  | 'MESSAGE_QUEUED'
  | 'NEW_MESSAGE'
  | 'NEW_PENDING'
  | 'PENDING_UPDATED'
  | 'PENDING_LIST'
  | 'APPROVE_MESSAGE'
  | 'REJECT_MESSAGE'
  | 'CLEAR_SCREEN'
  | 'SETTING_UPDATED'
  | 'SENDING_DISABLED'
  | 'MODE_CHANGE'
  | 'WS_CONNECTED'
  | 'WS_DISCONNECTED';
