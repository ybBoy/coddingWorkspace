// 类型定义文件
// 定义前后端通用的数据结构类型

// 用户角色
export type UserRole = 'user' | 'admin';

// 用户信息
export interface User {
  employeeId: string;
  userName: string;
  role: UserRole;
  loginAt: number;
  wsSessionId?: string;
}

// 场次状态
export type SessionStatus = 'active' | 'closed';

// 活动场次
export interface Session {
  id: string;
  name: string;
  date: string;       // yyyy-MM-dd
  startTime: string;
  endTime: string;
  capacity: number;
  status: SessionStatus;
  createdBy?: string;
  createdAt?: number;
  bookedCount: number;
  checkedInCount: number;
  waitlistCount: number;
}

// 预约记录
export interface Booking {
  id: string;
  sessionId: string;
  employeeId: string;   // 工号，唯一身份标识
  userName: string;
  phone?: string;
  status: 'booked' | 'waitlist' | 'checkedIn' | 'cancelled';
  createdAt: number;
}

// WebSocket 消息类型
export type MessageType =
  | 'init'
  | 'loginOk'
  | 'loginFail'
  | 'sessions'
  | 'bookingOk'
  | 'bookingFail'
  | 'cancelOk'
  | 'checkInOk'
  | 'sessionOk'
  | 'exportCsvOk'
  | 'activity'
  | 'error';

// WebSocket 消息结构
export interface WSMessage {
  type: MessageType;
  payload: any;
}

// 活动动态条目
export interface ActivityItem {
  id: string;
  time: string;
  type: 'booking' | 'cancel' | 'checkIn' | 'waitlist' | 'autoPromote' | 'sessionAdd' | 'sessionUpdate' | 'sessionStatus';
  userName: string;
  sessionName: string;
  message: string;
  promoted?: boolean;  // 候补转正时为 true，前端醒目提示
}

// 场次管理表单
export interface AdminSessionForm {
  sessionId?: string;
  name: string;
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
}

// 登录表单
export interface LoginForm {
  employeeId: string;
  userName: string;
}

// 操作日志（前端展示用）
export interface OperationLog {
  id: string;
  type: string;
  operator: string;
  detail: string;
  targetId: string;
  timestamp: number;
}
