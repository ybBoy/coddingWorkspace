import React, { useEffect, useState } from 'react';
import { UserRole } from '../types';
import { eventBus } from '../core/EventBus';

const STAFF_KEY = 'petboard_staff';
const ROLE_KEY = 'petboard_role';

const StaffSelector: React.FC = () => {
  const [staffName, setStaffName] = useState(() => localStorage.getItem(STAFF_KEY) || '');
  const [role, setRole] = useState<UserRole>(() => (localStorage.getItem(ROLE_KEY) as UserRole) || 'STAFF');
  const [editing, setEditing] = useState(false);
  const [inputName, setInputName] = useState('');

  useEffect(() => {
    eventBus.emit('staffChanged', { staffName, role });
  }, [staffName, role]);

  const handleSave = () => {
    const trimmed = inputName.trim();
    if (trimmed) {
      setStaffName(trimmed);
      localStorage.setItem(STAFF_KEY, trimmed);
    }
    setEditing(false);
  };

  const handleRoleToggle = () => {
    const newRole: UserRole = role === 'ADMIN' ? 'STAFF' : 'ADMIN';
    setRole(newRole);
    localStorage.setItem(ROLE_KEY, newRole);
  };

  if (!staffName || editing) {
    return (
      <div className="staff-selector">
        <div className="staff-input-row">
          <input
            type="text"
            className="form-input staff-input"
            value={inputName}
            onChange={(e) => setInputName(e.target.value)}
            placeholder="输入您的姓名"
            onKeyDown={(e) => e.key === 'Enter' && handleSave()}
          />
          <button className="btn-primary btn-sm" onClick={handleSave}>确认</button>
        </div>
      </div>
    );
  }

  return (
    <div className="staff-selector">
      <div className="staff-info">
        <span className="staff-name">👤 {staffName}</span>
        <button className={`role-badge ${role.toLowerCase()}`} onClick={handleRoleToggle}>
          {role === 'ADMIN' ? '👑 管理员' : '👤 员工'}
        </button>
        <button className="btn-icon" onClick={() => { setInputName(staffName); setEditing(true); }} title="修改姓名">✏️</button>
      </div>
    </div>
  );
};

export default StaffSelector;
