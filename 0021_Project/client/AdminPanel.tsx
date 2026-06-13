import React, { useState } from 'react';
import { Session, AdminSessionForm, User } from './types';
import { eventBus, EVENTS } from './EventBus';

// AdminPanel 场次管理面板（仅管理员可见）
// 职责：
//   1. 展示今天的所有场次
//   2. 新增场次
//   3. 编辑场次（名称、时间、容量）
//   4. 关闭/开放场次
//   5. 导出场次签到统计 CSV
interface Props {
  sessions: Session[];
  currentUser: User;
}

const AdminPanel: React.FC<Props> = ({ sessions, currentUser }) => {
  const [showAddForm, setShowAddForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState<AdminSessionForm>({
    name: '',
    date: new Date().toISOString().split('T')[0],
    startTime: '09:00',
    endTime: '10:00',
    capacity: 10,
  });

  const today = new Date().toISOString().split('T')[0];

  const handleAddClick = () => {
    setFormData({
      name: '',
      date: today,
      startTime: '09:00',
      endTime: '10:00',
      capacity: 10,
    });
    setEditingId(null);
    setShowAddForm(true);
  };

  const handleEdit = (session: Session) => {
    setFormData({
      sessionId: session.id,
      name: session.name,
      date: session.date,
      startTime: session.startTime,
      endTime: session.endTime,
      capacity: session.capacity,
    });
    setEditingId(session.id);
    setShowAddForm(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.startTime || !formData.endTime || formData.capacity <= 0) {
      alert('请填写完整的场次信息');
      return;
    }

    if (editingId) {
      eventBus.emit(EVENTS.SESSION_UPDATE_REQUEST, {
        ...formData,
        operator: currentUser.employeeId,
      });
    } else {
      eventBus.emit(EVENTS.SESSION_ADD_REQUEST, {
        ...formData,
        createdBy: currentUser.employeeId,
      });
    }
    setShowAddForm(false);
    setEditingId(null);
  };

  const handleToggleStatus = (session: Session) => {
    const close = session.status === 'active';
    const confirmMsg = close
      ? `确定关闭「${session.name}」吗？关闭后用户将无法预约。`
      : `确定开放「${session.name}」吗？`;
    if (!confirm(confirmMsg)) return;
    eventBus.emit(EVENTS.SESSION_CLOSE_REQUEST, {
      sessionId: session.id,
      status: close ? 'closed' : 'active',
      operator: currentUser.employeeId,
    });
  };

  const handleExportCsv = (session: Session) => {
    eventBus.emit(EVENTS.EXPORT_CSV_REQUEST, { sessionId: session.id });
  };

  const todaySessions = sessions.filter((s) => s.date === today);

  return (
    <div className="admin-panel">
      <div className="panel-header">
        <h3>⚙️ 场次管理（管理员）</h3>
        <button className="btn-add" onClick={handleAddClick}>
          + 新增场次
        </button>
      </div>

      {/* 新增/编辑表单 */}
      {showAddForm && (
        <div className="form-overlay">
          <div className="form-card">
            <h4>{editingId ? '编辑场次' : '新增场次'}</h4>
            <form onSubmit={handleSubmit} className="session-form">
              <div className="form-row">
                <label>场次名称</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="如：动感单车课"
                />
              </div>
              <div className="form-row">
                <label>日期</label>
                <input
                  type="date"
                  value={formData.date}
                  onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                />
              </div>
              <div className="form-row-inline">
                <div className="form-row">
                  <label>开始时间</label>
                  <input
                    type="time"
                    value={formData.startTime}
                    onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
                  />
                </div>
                <div className="form-row">
                  <label>结束时间</label>
                  <input
                    type="time"
                    value={formData.endTime}
                    onChange={(e) => setFormData({ ...formData, endTime: e.target.value })}
                  />
                </div>
                <div className="form-row">
                  <label>容量</label>
                  <input
                    type="number"
                    min="1"
                    value={formData.capacity}
                    onChange={(e) => setFormData({ ...formData, capacity: parseInt(e.target.value) || 0 })}
                  />
                </div>
              </div>
              <div className="form-actions">
                <button type="button" className="btn-cancel" onClick={() => setShowAddForm(false)}>
                  取消
                </button>
                <button type="submit" className="btn-save">
                  {editingId ? '保存修改' : '新增'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 场次列表 */}
      <div className="session-list">
        {todaySessions.length === 0 && (
          <p className="no-data">暂无今日场次</p>
        )}
        {todaySessions.map((session) => (
          <div
            key={session.id}
            className={`session-item ${session.status === 'closed' ? 'closed' : ''}`}
          >
            <div className="session-info">
              <div className="session-name">
                {session.name}
                {session.status === 'closed' && <span className="badge-closed">已关闭</span>}
              </div>
              <div className="session-meta">
                {session.startTime} - {session.endTime} · 容量 {session.capacity}
                <span className="stat-ok"> 已约 {session.bookedCount}</span>
                <span className="stat-checkin"> 签到 {session.checkedInCount}</span>
                <span className="stat-wait"> 候补 {session.waitlistCount}</span>
              </div>
            </div>
            <div className="session-actions">
              <button className="btn-export" title="导出签到CSV" onClick={() => handleExportCsv(session)}>
                📊 导出
              </button>
              <button
                className={`btn-toggle ${session.status === 'closed' ? 'open' : 'close'}`}
                onClick={() => handleToggleStatus(session)}
              >
                {session.status === 'closed' ? '开放' : '关闭'}
              </button>
              <button className="btn-edit" onClick={() => handleEdit(session)}>
                编辑
              </button>
            </div>
          </div>
        ))}
      </div>

      <style>{`
        .admin-panel {
          background: white;
          border-radius: 12px;
          padding: 16px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
          display: flex;
          flex-direction: column;
          gap: 12px;
          height: 100%;
          min-height: 0;
        }

        .panel-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .panel-header h3 {
          margin: 0;
          color: #1a73e8;
          font-size: 15px;
        }

        .btn-add {
          padding: 6px 14px;
          background: #1a73e8;
          color: white;
          border: none;
          border-radius: 6px;
          font-size: 13px;
          cursor: pointer;
          transition: background 0.2s;
        }
        .btn-add:hover { background: #1557b0; }

        /* 表单弹窗 */
        .form-overlay {
          position: fixed;
          top: 0; left: 0; right: 0; bottom: 0;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
        }

        .form-card {
          background: white;
          border-radius: 12px;
          padding: 24px;
          width: 90%;
          max-width: 500px;
          box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
        }

        .form-card h4 {
          margin: 0 0 16px 0;
          color: #1a73e8;
        }

        .session-form {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .form-row {
          display: flex;
          flex-direction: column;
          gap: 4px;
          flex: 1;
        }

        .form-row label {
          font-size: 12px;
          color: #5f6368;
          font-weight: 500;
        }

        .form-row input {
          padding: 8px 12px;
          border: 1px solid #dadce0;
          border-radius: 6px;
          font-size: 13px;
          outline: none;
        }
        .form-row input:focus {
          border-color: #1a73e8;
          box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.1);
        }

        .form-row-inline {
          display: flex;
          gap: 12px;
        }

        .form-actions {
          display: flex;
          justify-content: flex-end;
          gap: 8px;
          margin-top: 8px;
        }

        .btn-cancel {
          padding: 8px 16px;
          background: #f1f3f4;
          color: #3c4043;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          font-size: 13px;
        }
        .btn-cancel:hover { background: #e8eaed; }

        .btn-save {
          padding: 8px 16px;
          background: #1a73e8;
          color: white;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          font-size: 13px;
        }
        .btn-save:hover { background: #1557b0; }

        /* 场次列表 */
        .session-list {
          display: flex;
          flex-direction: column;
          gap: 8px;
          overflow-y: auto;
          flex: 1;
          min-height: 0;
        }

        .no-data {
          text-align: center;
          color: #80868b;
          padding: 20px;
          font-size: 13px;
        }

        .session-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 10px 12px;
          background: #f8f9fa;
          border-radius: 8px;
          border: 2px solid transparent;
          transition: all 0.2s;
        }

        .session-item.selected {
          border-color: #1a73e8;
          background: #e8f0fe;
        }

        .session-item.closed {
          opacity: 0.7;
          background: #f1f3f4;
        }

        .session-name {
          font-size: 14px;
          font-weight: 500;
          color: #3c4043;
          margin-bottom: 2px;
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .badge-closed {
          background: #d93025;
          color: white;
          font-size: 11px;
          padding: 2px 6px;
          border-radius: 4px;
          font-weight: normal;
        }

        .session-meta {
          font-size: 12px;
          color: #5f6368;
        }

        .stat-ok { color: #1a73e8; margin-left: 6px; }
        .stat-checkin { color: #188038; margin-left: 6px; }
        .stat-wait { color: #e37400; margin-left: 6px; }

        .session-actions {
          display: flex;
          gap: 6px;
        }

        .session-actions button {
          padding: 5px 10px;
          border: none;
          border-radius: 4px;
          font-size: 12px;
          cursor: pointer;
          transition: all 0.2s;
        }

        .btn-export {
          background: #e8f0fe;
          color: #1a73e8;
        }
        .btn-export:hover { background: #d2e3fc; }

        .btn-toggle.close {
          background: #fce8e6;
          color: #d93025;
        }
        .btn-toggle.close:hover { background: #fad2cf; }

        .btn-toggle.open {
          background: #e6f4ea;
          color: #188038;
        }
        .btn-toggle.open:hover { background: #ceead6; }

        .btn-edit {
          background: #f1f3f4;
          color: #3c4043;
        }
        .btn-edit:hover { background: #e8eaed; }
      `}</style>
    </div>
  );
};

export default AdminPanel;
