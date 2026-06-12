import React, { useState } from 'react';
import { Session, Booking } from './types';
import { eventBus, EVENTS } from './EventBus';

// BookingPanel 预约面板组件
// 职责：用户输入姓名后预约/取消预约场次，显示自己的预约状态
// 交互：输入姓名 → 点击预约 → 通过 EventBus 发出 BOOKING_REQUEST
//      输入姓名 → 点击取消 → 通过 EventBus 发出 CANCEL_REQUEST
// 接收：当前选中的 session 和我的预约列表（由 App 传入）

interface BookingPanelProps {
  session: Session | null;
  myBookings: Booking[];
}

const BookingPanel: React.FC<BookingPanelProps> = ({ session, myBookings }) => {
  const [name, setName] = useState('');

  // 查找当前场次下该姓名的预约
  const myBooking = session
    ? myBookings.find(
        (b) => b.sessionId === session.id && b.userName === name.trim()
      )
    : null;

  const handleBook = () => {
    const userName = name.trim();
    if (!userName) {
      alert('请输入姓名');
      return;
    }
    if (!session) {
      alert('请先选择场次');
      return;
    }
    eventBus.emit(EVENTS.BOOKING_REQUEST, {
      sessionId: session.id,
      userName,
    });
  };

  const handleCancel = () => {
    const userName = name.trim();
    if (!userName || !session || !myBooking) return;
    eventBus.emit(EVENTS.CANCEL_REQUEST, {
      bookingId: myBooking.id,
      userName,
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
          <div className="selected-session">
            <div className="session-title">{session.name}</div>
            <div className="session-time-range">
              {session.startTime} - {session.endTime}
            </div>
          </div>

          <div className="form-group">
            <label>姓名</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="请输入您的姓名"
              maxLength={20}
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
              <button className="btn btn-primary" onClick={handleBook}>
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
          gap: 14px;
        }
        .selected-session {
          background: #e8f0fe;
          padding: 12px;
          border-radius: 6px;
          border-left: 4px solid #1a73e8;
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
      `}</style>
    </div>
  );
};

export default BookingPanel;
