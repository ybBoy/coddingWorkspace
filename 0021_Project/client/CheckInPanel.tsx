import React, { useState, useMemo } from 'react';
import { Session, Booking } from './types';
import { eventBus, EVENTS } from './EventBus';

// CheckInPanel 签到面板组件（仅管理员可见）
// 职责：工作人员使用，搜索姓名/工号并标记签到
// 交互：输入关键词搜索 → 显示匹配的预约 → 点击签到按钮 → EventBus 发出 CHECKIN_REQUEST
// 接收：当前选中的 session 和该场次的所有预约列表

interface CheckInPanelProps {
  session: Session | null;
  bookings: Booking[];
}

const CheckInPanel: React.FC<CheckInPanelProps> = ({ session, bookings }) => {
  const [searchKeyword, setSearchKeyword] = useState('');

  // 筛选当前场次的预约，并按关键词搜索过滤（支持姓名或工号）
  const filteredBookings = useMemo(() => {
    if (!session) return [];
    const sessionBookings = bookings.filter(
      (b) => b.sessionId === session.id && b.status !== 'cancelled'
    );
    if (!searchKeyword.trim()) return sessionBookings;
    const keyword = searchKeyword.trim().toLowerCase();
    return sessionBookings.filter((b) =>
      b.userName.toLowerCase().includes(keyword) ||
      (b.employeeId && b.employeeId.toLowerCase().includes(keyword))
    );
  }, [session, bookings, searchKeyword]);

  // 按状态排序：已签到 > 已预约 > 候补
  const sortedBookings = useMemo(() => {
    const order: Record<string, number> = { checkedIn: 0, booked: 1, waitlist: 2, cancelled: 3 };
    return [...filteredBookings].sort((a, b) => {
      const orderA = order[a.status] ?? 99;
      const orderB = order[b.status] ?? 99;
      if (orderA !== orderB) {
        return orderA - orderB;
      }
      return a.userName.localeCompare(b.userName);
    });
  }, [filteredBookings]);

  const handleCheckIn = (bookingId: string) => {
    eventBus.emit(EVENTS.CHECKIN_REQUEST, { bookingId });
  };

  const totalCount = session
    ? bookings.filter((b) => b.sessionId === session.id && b.status !== 'cancelled').length
    : 0;

  return (
    <div className="checkin-panel">
      <h2>✅ 签到管理（管理员）</h2>

      {!session ? (
        <div className="placeholder">
          <p>👈 请从左侧选择一个场次</p>
        </div>
      ) : (
        <div className="checkin-content">
          <div className="session-summary">
            <div className="summary-title">{session.name}</div>
            <div className="summary-stats">
              <span className="chip chip-booked">
                预约 {session.bookedCount}/{session.capacity}
              </span>
              <span className="chip chip-checked">
                签到 {session.checkedInCount}
              </span>
              {session.waitlistCount > 0 && (
                <span className="chip chip-waitlist">
                  候补 {session.waitlistCount}
                </span>
              )}
            </div>
          </div>

          <div className="search-box">
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder="🔍 输入姓名或工号搜索..."
            />
          </div>

          <div className="booking-list">
            {sortedBookings.length === 0 && (
              <p className="empty-list">暂无预约记录</p>
            )}
            {sortedBookings.map((booking) => (
              <div
                key={booking.id}
                className={`booking-item status-${booking.status}`}
              >
                <div className="booking-info">
                  <div className="user-row">
                    <span className="user-name">{booking.userName}</span>
                    <span className="user-emp-id">({booking.employeeId})</span>
                  </div>
                  <span className={`status-tag tag-${booking.status}`}>
                    {booking.status === 'booked' && '已预约'}
                    {booking.status === 'waitlist' && '候补中'}
                    {booking.status === 'checkedIn' && '已签到'}
                  </span>
                </div>
                {booking.status !== 'checkedIn' &&
                  booking.status !== 'waitlist' && (
                    <button
                      className="btn-checkin"
                      onClick={() => handleCheckIn(booking.id)}
                    >
                      签到
                    </button>
                  )}
                {booking.status === 'checkedIn' && (
                  <span className="checked-mark">✓</span>
                )}
              </div>
            ))}
          </div>

          <div className="list-footer">
            共 {totalCount} 条记录
            {searchKeyword && ` (匹配 ${sortedBookings.length} 条)`}
          </div>
        </div>
      )}

      <style>{`
        .checkin-panel {
          background: white;
          border-radius: 8px;
          padding: 16px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
          height: 100%;
          display: flex;
          flex-direction: column;
        }
        .checkin-panel h2 {
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
        .checkin-content {
          flex: 1;
          display: flex;
          flex-direction: column;
          min-height: 0;
        }
        .session-summary {
          background: #f8f9fa;
          padding: 10px 12px;
          border-radius: 6px;
          margin-bottom: 10px;
        }
        .summary-title {
          font-weight: 600;
          font-size: 14px;
          margin-bottom: 6px;
        }
        .summary-stats {
          display: flex;
          gap: 8px;
          flex-wrap: wrap;
        }
        .chip {
          font-size: 12px;
          padding: 2px 8px;
          border-radius: 10px;
          font-weight: 500;
        }
        .chip-booked {
          background: #e8f0fe;
          color: #1a73e8;
        }
        .chip-checked {
          background: #e6f4ea;
          color: #188038;
        }
        .chip-waitlist {
          background: #fef7e0;
          color: #e37400;
        }
        .search-box {
          margin-bottom: 10px;
        }
        .search-box input {
          width: 100%;
          padding: 8px 12px;
          border: 1px solid #dadce0;
          border-radius: 6px;
          font-size: 13px;
          outline: none;
        }
        .search-box input:focus {
          border-color: #1a73e8;
          box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.1);
        }
        .booking-list {
          flex: 1;
          overflow-y: auto;
          display: flex;
          flex-direction: column;
          gap: 6px;
        }
        .booking-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 8px 10px;
          border-radius: 6px;
          background: #f8f9fa;
          border-left: 3px solid #dadce0;
        }
        .booking-item.status-booked {
          border-left-color: #1a73e8;
        }
        .booking-item.status-waitlist {
          border-left-color: #e37400;
          background: #fff9e6;
        }
        .booking-item.status-checkedIn {
          border-left-color: #188038;
          background: #e6f4ea;
        }
        .booking-info {
          display: flex;
          flex-direction: column;
          gap: 2px;
        }
        .user-row {
          display: flex;
          align-items: baseline;
          gap: 4px;
        }
        .user-name {
          font-size: 14px;
          font-weight: 500;
        }
        .user-emp-id {
          font-size: 12px;
          color: #5f6368;
        }
        .status-tag {
          font-size: 11px;
          padding: 2px 6px;
          border-radius: 4px;
          align-self: flex-start;
        }
        .tag-booked {
          background: #e8f0fe;
          color: #1a73e8;
        }
        .tag-waitlist {
          background: #fef7e0;
          color: #e37400;
        }
        .tag-checkedIn {
          background: #e6f4ea;
          color: #188038;
        }
        .btn-checkin {
          background: #188038;
          color: white;
          border: none;
          padding: 4px 12px;
          border-radius: 4px;
          font-size: 12px;
          cursor: pointer;
          transition: background 0.2s;
        }
        .btn-checkin:hover {
          background: #137333;
        }
        .checked-mark {
          color: #188038;
          font-size: 18px;
          font-weight: bold;
        }
        .empty-list {
          text-align: center;
          color: #80868b;
          padding: 30px 0;
          font-size: 13px;
        }
        .list-footer {
          margin-top: 8px;
          padding-top: 8px;
          border-top: 1px solid #e0e0e0;
          font-size: 12px;
          color: #5f6368;
          text-align: center;
        }
      `}</style>
    </div>
  );
};

export default CheckInPanel;
