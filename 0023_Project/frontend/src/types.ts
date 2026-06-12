export interface DanmakuMessage {
  id: string;
  content: string;
  nickname: string;
  timestamp: number;
  status: 'pending' | 'approved' | 'rejected' | 'error';
  sensitive: boolean;
  color: string;
  pinned: boolean;
}

export interface OperationLog {
  timestamp: number;
  action: string;
  operator: string;
  detail: string;
}

export interface Settings {
  sendingEnabled: boolean;
  playbackPaused: boolean;
  eventTitle: string;
  welcomeMessage: string;
  colorTheme: string;
  customColors: string[] | null;
  sensitiveWords: string[];
  moderatorPassword: string;
  speedMin: number;
  speedMax: number;
  fontSize: number;
  trackCount: number;
  pendingCount?: number;
  onlineCount?: number;
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
  | 'SEND_REJECTED'
  | 'MODE_CHANGE'
  | 'TOGGLE_SENDING'
  | 'SET_ROLE'
  | 'GET_PENDING'
  | 'GET_HISTORY'
  | 'HISTORY_MESSAGES'
  | 'AUTH_FAILED'
  | 'AUTH_SUCCESS'
  | 'VALIDATE_TOKEN'
  | 'TOGGLE_PLAYBACK'
  | 'TOGGLE_PIN'
  | 'PIN_UPDATED'
  | 'APPROVE_NORMAL_ONLY'
  | 'UPDATE_SETTINGS'
  | 'GET_LOGS'
  | 'OPERATION_LOGS'
  | 'EXPORT_DATA'
  | 'EXPORT_DONE'
  | 'ROTATE_BACKUP'
  | 'BACKUP_DONE'
  | 'ONLINE_COUNT'
  | 'PLAYBACK_STATE'
  | 'WS_CONNECTED'
  | 'WS_DISCONNECTED';
