import { useEffect, useState } from 'react';
import { eventBus, Events } from '../base/EventBus';
import { wsClient } from '../base/wsClient';
import type { Room, Operator } from '../base/types';

type EditState = {
  roomId: string;
  roomNo: string;
  floor: number;
  type: string;
  defaultPrice: number;
} | null;

type AddState = {
  roomNo: string;
  floor: number;
  type: string;
  defaultPrice: number;
};

const ROOM_TYPES = ['标准间', '大床房', '豪华间', '家庭房', '总统套房'];

export function AdminPage() {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [operators, setOperators] = useState<Operator[]>([]);
  const [currentOperator, setCurrentOperator] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);
  const [editState, setEditState] = useState<EditState>(null);
  const [addState, setAddState] = useState<AddState>({
  roomNo: '',
  floor: 1,
  type: '标准间',
  defaultPrice: 198,
});

  useEffect(() => {
    const roomsHandler = (data: Room[]) => setRooms(data);
    const operatorsHandler = (data: Operator[]) => setOperators(data);
    const operatorHandler = (name: string) => setCurrentOperator(name);

    eventBus.on(Events.ROOMS_UPDATED, roomsHandler);
    eventBus.on(Events.OPERATORS_UPDATED, operatorsHandler);
    eventBus.on(Events.OPERATOR_CHANGED, operatorHandler);

    return () => {
      eventBus.off(Events.ROOMS_UPDATED, roomsHandler);
      eventBus.off(Events.OPERATORS_UPDATED, operatorsHandler);
      eventBus.off(Events.OPERATOR_CHANGED, operatorHandler);
    };
  }, []);

  const handleOperatorChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const name = e.target.value;
    setCurrentOperator(name);
    wsClient.setCurrentOperator(name);
  };

  const handleAddRoom = () => {
    if (!addState.roomNo.trim() === '') {
      alert('请输入房间号');
      return;
    }
    wsClient.addRoom(
      addState.roomNo.trim(),
      addState.floor,
      addState.type,
      addState.defaultPrice,
    );
    setShowAddForm(false);
    setAddState({ roomNo: '', floor: 1, type: '标准间', defaultPrice: 198 });
  };

  const handleUpdateRoom = () => {
    if (!editState || editState.roomNo.trim() === '') {
      alert('请输入房间号');
      return;
    }
    wsClient.updateRoom(
      editState.roomId,
      editState.roomNo.trim(),
      editState.floor,
      editState.type,
      editState.defaultPrice,
    );
    setEditState(null);
  };

  const handleDeleteRoom = (roomId: string, roomNo: string, status: string) => {
    if (status === 'OCCUPIED') {
      alert('该房间有客人入住，无法删除');
      return;
    }
    if (confirm(`确定删除房间 ${roomNo}？`)) {
      wsClient.deleteRoom(roomId);
    }
  };

  const startEdit = (room: Room) => {
    setEditState({
      roomId: room.id,
      roomNo: room.roomNo,
      floor: room.floor,
      type: room.type,
      defaultPrice: room.defaultPrice,
    });
  };

  const floors = Array.from(new Set(rooms.map((r) => r.floor))).sort((a, b) => a - b);

  return (
    <div className="admin-page">
      <div className="admin-header">
        <h2>⚙️ 房间管理</h2>
        <div className="operator-selector">
          <label>当前操作人：</label>
          <select value={currentOperator} onChange={handleOperatorChange}>
            {operators.map((op) => (
              <option key={op.id} value={op.name}>
                {op.name}（{op.role === 'manager' ? '经理' : '前台'}）
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="admin-toolbar">
        <button className="btn btn-primary" onClick={() => setShowAddForm(true)}>
          ➕ 新增房间
        </button>
        <span className="room-count">共 {rooms.length} 间客房</span>
      </div>

      {showAddForm && (
        <div className="form-card">
          <h3>新增房间</h3>
          <div className="form-grid">
            <div className="form-group">
              <label>房间号</label>
              <input
                type="text"
                value={addState.roomNo}
                placeholder="如：101"
                onChange={(e) => setAddState({ ...addState, roomNo: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>楼层</label>
              <select
                value={addState.floor}
                onChange={(e) => setAddState({ ...addState, floor: parseInt(e.target.value) })}
              >
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((f) => (
                  <option key={f} value={f}>{f} 楼</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>房型</label>
              <select
                value={addState.type}
                onChange={(e) => setAddState({ ...addState, type: e.target.value })}
              >
                {ROOM_TYPES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>默认房价（元）</label>
              <input
                type="number"
                value={addState.defaultPrice}
                onChange={(e) => setAddState({ ...addState, defaultPrice: parseFloat(e.target.value) || 0 })}
              />
            </div>
          </div>
          <div className="form-actions">
            <button className="btn btn-primary" onClick={handleAddRoom}>
              保存
            </button>
            <button className="btn btn-secondary" onClick={() => setShowAddForm(false)}>
              取消
            </button>
          </div>
        </div>
      )}

      {editState && (
        <div className="form-card">
          <h3>修改房间信息</h3>
          <div className="form-grid">
            <div className="form-group">
              <label>房间号</label>
              <input
                type="text"
                value={editState.roomNo}
                onChange={(e) => setEditState({ ...editState, roomNo: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>楼层</label>
              <select
                value={editState.floor}
                onChange={(e) => setEditState({ ...editState, floor: parseInt(e.target.value) })}
              >
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((f) => (
                  <option key={f} value={f}>{f} 楼</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>房型</label>
              <select
                value={editState.type}
                onChange={(e) => setEditState({ ...editState, type: e.target.value })}
              >
                {ROOM_TYPES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>默认房价（元）</label>
              <input
                type="number"
                value={editState.defaultPrice}
                onChange={(e) => setEditState({ ...editState, defaultPrice: parseFloat(e.target.value) || 0 })}
              />
            </div>
          </div>
          <div className="form-actions">
            <button className="btn btn-primary" onClick={handleUpdateRoom}>
              保存
            </button>
            <button className="btn btn-secondary" onClick={() => setEditState(null)}>
              取消
            </button>
          </div>
        </div>
      )}

      <div className="room-management-grid">
        {floors.map((floor) => (
          <div key={floor} className="floor-section">
            <h4>{floor} 楼</h4>
            <div className="floor-rooms">
              {rooms
                .filter((r) => r.floor === floor)
                .sort((a, b) => a.roomNo.localeCompare(b.roomNo))
                .map((room) => (
                  <div key={room.id} className={`room-admin-card status-${room.status.toLowerCase()}`}>
                    <div className="room-admin-header">
                      <span className="room-no">{room.roomNo}</span>
                      <span className="room-type">{room.type}</span>
                    </div>
                    <div className="room-admin-info">
                      <div className="price">¥{room.defaultPrice.toFixed(0)}</div>
                      <div className="status">{statusText[room.status]}</div>
                    </div>
                    <div className="room-admin-actions">
                      <button
                        className="btn btn-sm btn-secondary"
                        onClick={() => startEdit(room)}
                      >
                        ✏️ 修改
                      </button>
                      {room.status === 'DISABLED' ? (
                        <button
                          className="btn btn-sm btn-success"
                          onClick={() => {
                            if (confirm(`确定启用房间 ${room.roomNo}？启用后状态变为待打扫`)) {
                              wsClient.enableRoom(room.id);
                            }
                          }}
                        >
                          ▶️ 启用
                        </button>
                      ) : (
                        <button
                          className="btn btn-sm btn-warning"
                          disabled={room.status === 'OCCUPIED'}
                          onClick={() => {
                            if (confirm(`确定停用房间 ${room.roomNo}？`)) {
                              wsClient.disableRoom(room.id);
                            }
                          }}
                        >
                          ⏸️ 停用
                        </button>
                      )}
                      <button
                        className="btn btn-sm btn-danger"
                        disabled={room.status === 'OCCUPIED'}
                        onClick={() => handleDeleteRoom(room.id, room.roomNo, room.status)}
                      >
                        🗑️ 删除
                      </button>
                    </div>
                  </div>
                ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

const statusText: Record<string, string> = {
  VACANT: '空房',
  OCCUPIED: '入住中',
  DIRTY: '待打扫',
  MAINTENANCE: '维修中',
  DISABLED: '已停用',
};
