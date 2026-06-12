/**
 * CounterConfig 窗口配置组件
 * 职责：管理窗口配置，包括新增窗口、修改窗口名、配置支持的业务类型、启用/停用窗口
 * 通过 EventBus 发布配置变更事件到后端
 */
import React, { useEffect, useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { QueueState, Counter, BusinessType, BUSINESS_TYPES } from './types';

const CounterConfig: React.FC = () => {
  const [counters, setCounters] = useState<Counter[]>([]);
  const [newName, setNewName] = useState('');
  const [newTypes, setNewTypes] = useState<BusinessType[]>(['咨询', '办理', '售后']);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [editTypes, setEditTypes] = useState<BusinessType[]>([]);

  useEffect(() => {
    const unsub = eventBus.on(EVENTS.QUEUE_STATE_UPDATED, (state: QueueState) => {
      setCounters(state.counters || []);
    });
    return unsub;
  }, []);

  const toggleNewType = (t: BusinessType) => {
    setNewTypes((prev) =>
      prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t]
    );
  };

  const toggleEditType = (t: BusinessType) => {
    setEditTypes((prev) =>
      prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t]
    );
  };

  const handleAdd = () => {
    if (!newName.trim() || newTypes.length === 0) return;
    eventBus.emit(EVENTS.ADD_COUNTER, { name: newName.trim(), supportedBusinessTypes: newTypes });
    setNewName('');
    setNewTypes(['咨询', '办理', '售后']);
  };

  const startEdit = (c: Counter) => {
    setEditingId(c.id);
    setEditName(c.name);
    setEditTypes([...c.supportedBusinessTypes]);
  };

  const cancelEdit = () => {
    setEditingId(null);
  };

  const saveEdit = () => {
    if (!editingId || !editName.trim() || editTypes.length === 0) return;
    eventBus.emit(EVENTS.UPDATE_COUNTER, {
      counterId: editingId,
      name: editName.trim(),
      supportedBusinessTypes: editTypes,
    });
    setEditingId(null);
  };

  const handleToggle = (counterId: string, enabled: boolean) => {
    eventBus.emit(EVENTS.TOGGLE_COUNTER, { counterId, enabled });
  };

  return (
    <section className="counter-config">
      <h2 className="section-title">窗口配置</h2>

      <div className="config-list">
        {counters.map((c) => (
          <div key={c.id} className={`config-item ${c.enabled ? '' : 'disabled'}`}>
            {editingId === c.id ? (
              <div className="edit-form">
                <input
                  className="input"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  placeholder="窗口名称"
                />
                <div className="type-checkboxes">
                  {BUSINESS_TYPES.map((t) => (
                    <label key={t} className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={editTypes.includes(t)}
                        onChange={() => toggleEditType(t)}
                      />
                      {t}
                    </label>
                  ))}
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn btn-mini btn-success" onClick={saveEdit}>保存</button>
                  <button className="btn btn-mini" onClick={cancelEdit}>取消</button>
                </div>
              </div>
            ) : (
              <>
                <div className="counter-info">
                  <div className="counter-name">{c.name}</div>
                  <div className="counter-types">
                    {c.supportedBusinessTypes.map((t) => (
                      <span key={t} className="business-tag">{t}</span>
                    ))}
                  </div>
                </div>
                <div className="config-actions">
                  <button className="btn btn-mini" onClick={() => startEdit(c)}>编辑</button>
                  <button
                    className={`btn btn-mini ${c.enabled ? 'btn-warning' : 'btn-success'}`}
                    onClick={() => handleToggle(c.id, !c.enabled)}
                  >
                    {c.enabled ? '停用' : '启用'}
                  </button>
                </div>
              </>
            )}
          </div>
        ))}
      </div>

      <div className="add-counter-form">
        <h3 className="subsection-title">新增窗口</h3>
        <input
          className="input"
          placeholder="窗口名称，如：4号窗口"
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
        />
        <div className="type-checkboxes">
          {BUSINESS_TYPES.map((t) => (
            <label key={t} className="checkbox-label">
              <input
                type="checkbox"
                checked={newTypes.includes(t)}
                onChange={() => toggleNewType(t)}
              />
              {t}
            </label>
          ))}
        </div>
        <button
          className="btn btn-primary"
          onClick={handleAdd}
          disabled={!newName.trim() || newTypes.length === 0}
        >
          新增窗口
        </button>
      </div>
    </section>
  );
};

export default CounterConfig;
