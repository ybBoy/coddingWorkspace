import { eventBus, Events } from '../base/EventBus';
import { RoomStatusText, type Room } from '../base/types';
import './RoomGridPanel.css';

interface RoomGridPanelProps {
  rooms: Room[];
  selectedRoomId: string | null;
}

function formatTime(timestamp: number): string {
  const date = new Date(timestamp);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${month}-${day} ${hours}:${minutes}`;
}

function getStatusClass(status: string): string {
  switch (status) {
    case 'VACANT':
      return 'room-card status-vacant';
    case 'OCCUPIED':
      return 'room-card status-occupied';
    case 'DIRTY':
      return 'room-card status-dirty';
    case 'MAINTENANCE':
      return 'room-card status-maintenance';
    default:
      return 'room-card';
  }
}

function formatOverdue(room: Room): string | null {
  if (!room.isOverdue || !room.currentStay) {
    return null;
  }
  const now = Date.now();
  const expected = room.currentStay.expectedCheckOutTime;
  const diffMs = now - expected;
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  if (diffHours < 1) {
    const diffMins = Math.floor(diffMs / (1000 * 60));
    return `延时 ${diffMins} 分钟未退房`;
  }
  return `延时 ${diffHours} 小时未退房`;
}

export function RoomGridPanel({ rooms, selectedRoomId }: RoomGridPanelProps) {
  const groupedByFloor: Record<number, Room[]> = {};
  rooms.forEach((room) => {
    if (!groupedByFloor[room.floor]) {
      groupedByFloor[room.floor] = [];
    }
    groupedByFloor[room.floor].push(room);
  });

  const floors = Object.keys(groupedByFloor)
    .map(Number)
    .sort((a, b) => a - b);

  const handleRoomClick = (room: Room) => {
    eventBus.emit(Events.ROOM_SELECTED, room);
  };

  if (rooms.length === 0) {
    return (
      <div className="room-grid-empty">
        <p>暂无房间数据，请检查后端服务是否启动。</p>
      </div>
    );
  }

  return (
    <div className="room-grid-panel">
      {floors.map((floor) => (
        <div key={floor} className="floor-section">
          <div className="floor-title">{floor} 楼</div>
          <div className="room-grid">
            {groupedByFloor[floor]
              .sort((a, b) => a.roomNo.localeCompare(b.roomNo))
              .map((room) => {
                const overdueText = formatOverdue(room);
                return (
                  <div
                    key={room.id}
                    className={`${getStatusClass(room.status)} ${selectedRoomId === room.id ? 'selected' : ''}`}
                    onClick={() => handleRoomClick(room)}
                  >
                    <div className="room-no">{room.roomNo}</div>
                    <div className="room-type">{room.type}</div>
                    <div className="room-price">¥{room.defaultPrice}</div>
                    <div className="room-status">{RoomStatusText[room.status]}</div>
                    {room.currentStay && (
                      <div className="room-guest">
                        <div className="guest-name">{room.currentStay.guestName}</div>
                        <div className="checkout-time">
                          预计：{formatTime(room.currentStay.expectedCheckOutTime)}
                        </div>
                        <div className={`settle-status ${room.currentStay.settled ? 'settled' : 'unsettled'}`}>
                          {room.currentStay.settled ? '已结清' : '未结清'}
                        </div>
                      </div>
                    )}
                    {overdueText && <div className="room-overdue">{overdueText}</div>}
                  </div>
                );
              })}
          </div>
        </div>
      ))}
    </div>
  );
}
