// 类型定义文件
// 定义前后端通用的数据结构类型

// 活动场次
export interface Session {
  id: string;
  name: string;
  startTime: string;
  endTime: string;
  capacity: number;
  bookedCount: number;
  checkedInCount: number;
  waitlistCount: number;
}

// 预约记录
export interface Booking {
  id: string;
  sessionId: string;
  userName: string;
  status: 'booked' | 'waitlist' | 'checkedIn' | 'cancelled';
  createdAt: number;
}

// WebSocket 消息类型
export type MessageType =
  | 'init'          // 初始化数据
  | 'sessions'      // 场次列表更新
  | 'bookingOk'     // 预约成功
  | 'bookingFail'   // 预约失败
  | 'cancelOk'      // 取消成功
  | 'checkInOk'     // 签到成功
  | 'activity'      // 活动动态
  | 'error';        // 错误

// WebSocket 消息结构
export interface WSMessage {
  type: MessageType;
  payload: any;
}

// 活动动态条目
export interface ActivityItem {
  id: string;
  time: string;
  type: 'booking' | 'cancel' | 'checkIn' | 'waitlist' | 'autoPromote';
  userName: string;
  sessionName: string;
  message: string;
}
