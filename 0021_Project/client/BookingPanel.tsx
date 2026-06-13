import React, { useState, useMemo } from 'react';
import { Session, Booking, User } from './types';
import { eventBus, EVENTS } from './EventBus';

// BookingPanel 预约面板组件
// 职责：用户输入工号+姓名后预约/取消预约场次，显示自己的预约状态
// 交互：输入信息 → 点击预约 → 通过 EventBus 发出 BOOKING_REQUEST
//      输入信息 → 点击取消 → 通过 EventBus 发出 CANCEL_REQUEST
// 接收：当前选中的 session、当前登录用户、我的预约列表（由 App 传入）

interface BookingPanelProps {
  session: Session | null;
  myBookings: Booking[];
  currentUser: User | null;
}

const BookingPanel: React.FC<BookingPanelProps> = ({ session, myBookings, currentUser }) => {
  const [employeeId, setEmployeeId] = useState(currentUser?.employeeId || '');
  const [userName, setUserName] = useState(currentUser?.userName || '');
  const [phone, setPhone] = useState('');

  // 已登录普通用户不能修改自己的工号和姓名
  const isReadOnly = currentUser?.role === 'user';

  // 登录时自动填充
  React.useEffect(() => {
    if (currentUser) {
      setEmployeeId(currentUser.employeeId);
      setUserName(currentUser.userName);
    }
  }, [currentUser]);

  // 查找当前场次下该工号的预约
  const myBooking = useMemo(() => {
    if (!session || !employeeId.trim()) return null;
    return myBookings.find(
      (b) => b.sessionId === session.id && b.employeeId === employeeId.trim()
    );
  }, [session, myBookings, employeeId]);

  const handleBook = () => {
    const empId = employeeId.trim();
    const name = userName.trim();
    if (!empId) {
      alert('请输入工号');
      return;
    }
    if (!name) {
      alert('请输入姓名');
      return;
    }
    if (!session) {
      alert('请先选择场次');
      return;
    }
    if (session.status === 'closed') {
      alert('本场次已关闭预约');
      return;
    }
    eventBus.emit(EVENTS.BOOKING_REQUEST, {
      sessionId: session.id,
      employeeId: empId,
      userName: name,
      phone: phone.trim() || undefined,
    });
  };

  const handleCancel = () => {
    const empId = employeeId.trim();
    if (!empId || !session || !myBooking) return;
    eventBus.emit(EVENTS.CANCEL_REQUEST, {
      bookingId: myBooking.id,
      employeeId: empId,
    });
  };

  const isBooked = myBooking && myBooking.status === 'booked';
  const isWaitlist = myBooking && myBooking.status === 'waitlist';
  const isCheckedIn = myBooking && myBooking.status === 'checkedIn';

  return (
    <div className="booking-panel">
      <h2>📝 预约 / 取消</h2>

      {!session ? (
        <div className="placeholder">
          <p>👈 请从左侧选择一个场次</p>
        </div>
      ) : (
        <div className="booking-form">
          <div className={`selected-session ${session.status === 'closed' ? 'closed' : ''}`}>
            <div className="session-title">{session.name}</div>
            <div className="session-time-range">
              {session.startTime} - {session.endTime}
            </div>
            {session.status === 'closed' && (
              <div className="session-closed-badge">🔒 已关闭预约</div>
            )}
          </div>

          <div className="form-group">
            <label>工号（必填，唯一身份）</label>
            <input
              type="text"
              value={employeeId}
              onChange={(e) => setEmployeeId(e.target.value)}
              placeholder="请输入您的工号"
              maxLength={20}
              readOnly={isReadOnly}
              className={isReadOnly ? 'readonly' : ''}
            />
          </div>

          <div className="form-group">
            <label>姓名（必填）</label>
            <input
              type="text"
              value={userName}
              onChange={(e) => setUserName(e.target.value)}
              placeholder="请输入您的姓名"
              maxLength={20}
              readOnly={isReadOnly}
              className={isReadOnly ? 'readonly' : ''}
            />
          </div>

          <div className="form-group">
            <label>手机号（选填）</label>
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="选填，用于活动通知"
              maxLength={11}
            />
          </div>

          {myBooking && (
            <div className={`status-badge status-${myBooking.status}`}>
              {isBooked && '✅ 已预约'}
              {isWaitlist && '⏳ 候补中'}
              {isCheckedIn && '✅ 已签到'}
            </div>
          )}

          <div className="actions">
            {!myBooking || myBooking.status === 'cancelled' ? (
              <button
                className="btn btn-primary"
                onClick={handleBook}
                disabled={session.status === 'closed'}
              >
                {session.bookedCount >= session.capacity
                  ? '加入候补'
                  : '立即预约'}
              </button>
            ) : (
              <button
                className="btn btn-danger"
                onClick={handleCancel}
                disabled={!!isCheckedIn}
              >
                {isCheckedIn ? '已签到无法取消' : '取消预约'}
              </button>
            )}
          </div>

          <div className="tips">
            <p>💡 提示：场次满员后新预约自动进入候补队列</p>
            <p>💡 有人取消时，候补第一位自动转为正式预约</p>
            {currentUser && (
              <p className="current-user-info">
                👤 当前登录：{currentUser.userName}（{currentUser.role === 'admin' ? '管理员' : '普通用户'}）
              </p>
            )}
          </div>
        </div>
      )}

      <style>{`
        .booking-panel {
          background: white;
          border-radius: 8px;
          padding: 16px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
          height: 100%;
          display: flex;
          flex-direction: column;
        }
        .booking-panel h2 {
          font-size: 18px;
          color: #1a73e8;
          margin-bottom: 12px;
          border-bottom: 2px solid #e8f0fe;
          padding-bottom: 8px;
        }
        .placeholder {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #80868b;
          font-size: 14px;
        }
        .booking-form {
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 10px;
        }
        .selected-session {
          background: #e8f0fe;
          padding: 12px;
          border-radius: 6px;
          border-left: 4px solid #1a73e8;
        }
        .selected-session.closed {
          background: #fce8e6;
          border-left-color: #d93025;
        }
        .session-title {
          font-weight: 600;
          font-size: 15px;
          color: #202124;
        }
        .session-time-range {
          font-size: 13px;
          color: #5f6368;
          margin-top: 4px;
        }
        .session-closed-badge {
          margin-top: 6px;
          font-size: 12px;
          color: #d93025;
          font-weight: 500;
        }
        .form-group {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }
        .form-group label {
          font-size: 13px;
          color: #5f6368;
          font-weight: 500;
        }
        .form-group input {
          padding: 10px 12px;
          border: 1px solid #dadce0;
          border-radius: 6px;
          font-size: 14px;
          outline: none;
          transition: border-color 0.2s;
        }
        .form-group input.readonly {
          background: #f5f5f5;
          color: #5f6368;
        }
        .form-group input:focus {
          border-color: #1a73e8;
          box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.1);
        }
        .status-badge {
          text-align: center;
          padding: 8px;
          border-radius: 6px;
          font-weight: 500;
          font-size: 14px;
        }
        .status-booked {
          background: #e6f4ea;
          color: #188038;
        }
        .status-waitlist {
          background: #fef7e0;
          color: #e37400;
        }
        .status-checkedIn {
          background: #e6f4ea;
          color: #188038;
        }
        .actions {
          display: flex;
          gap: 10px;
        }
        .btn {
          flex: 1;
          padding: 10px 16px;
          border: none;
          border-radius: 6px;
          font-size: 14px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s;
        }
        .btn:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
        .btn-primary {
          background: #1a73e8;
          color: white;
        }
        .btn-primary:hover:not(:disabled) {
          background: #1557b0;
        }
        .btn-danger {
          background: #d93025;
          color: white;
        }
        .btn-danger:hover:not(:disabled) {
          background: #b3261e;
        }
        .tips {
          margin-top: auto;
          padding: 10px;
          background: #f1f3f4;
          border-radius: 6px;
        }
        .tips p {
          font-size: 12px;
          color: #5f6368;
          margin: 4px 0;
        }
        .current-user-info {
          margin-top: 6px;
          padding-top: 6px;
          border-top: 1px solid #e0e0e0;
          color: #1a73e8 !important;
          font-weight: 500;
        }
      `}</style>
    </div>
  );
};

export default BookingPanel;
