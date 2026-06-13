export interface Participant {
  id: string;
  name: string;
  groupId?: string | null;
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

export interface AppState {
  activityName: string;
  groupCount: number;
  participants: Participant[];
  groups: Group[];
  logs: ActionLog[];
  isHost: boolean;
}

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected';
