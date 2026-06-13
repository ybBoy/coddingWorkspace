export interface Task {
  id: string;
  title: string;
  description: string;
  priority: 'high' | 'medium' | 'low';
  status: 'pending' | 'claimed' | 'in_progress' | 'completed';
  assignee: string | null;
  createdAt: number;
  claimedAt: number;
}

export interface TaskLog {
  id: string;
  action: string;
  taskTitle: string;
  nickname: string;
  timestamp: number;
}

export interface WsMessage {
  type: 'init' | 'update';
  tasks: Task[];
  logs: TaskLog[];
}

export type PriorityFilter = 'all' | 'high' | 'medium' | 'low';
