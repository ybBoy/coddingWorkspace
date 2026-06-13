export interface Participant {
  id: string;
  name: string;
  groupId?: string | null;
  gender?: string | null;
  department?: string | null;
  skill?: number;
  tag?: string | null;
  selfRegistered?: boolean;
}

export interface Group {
  id: string;
  name: string;
  locked: boolean;
  participantIds: string[];
}

export interface ActionLog {
  timestamp: number;
  action: string;
  description: string;
  groupsSnapshot: Group[];
}

export interface GroupRule {
  type: string;
  value: string;
}

export interface AppState {
  activityName: string;
  groupCount: number;
  roomCode: string;
  participants: Participant[];
  groups: Group[];
  logs: ActionLog[];
  rules: GroupRule[];
  isHost: boolean;
}

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected';

export type AppPhase = 'lobby' | 'room';
