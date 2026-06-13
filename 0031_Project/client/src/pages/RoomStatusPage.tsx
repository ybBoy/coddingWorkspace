import { useEffect, useState } from 'react';
import { eventBus, Events } from '../base/EventBus';
import { wsClient } from '../base/wsClient';
import { FilterPanel } from '../panels/FilterPanel';
import { RoomGridPanel } from '../panels/RoomGridPanel';
import { CheckInPanel } from '../panels/CheckInPanel';
import { LogPanel } from '../panels/LogPanel';
import type { Room, RoomLog, RoomStatus } from '../base/types';
import './RoomStatusPage.css';

interface FilterState {
  floor: number | 'all';
  status: RoomStatus | 'all';
}

export function RoomStatusPage() {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [logs, setLogs] = useState<RoomLog[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<Room | null>(null);
  const [filter, setFilter] = useState<FilterState>({ floor: 'all', status: 'all' });
  const [wsConnected, setWsConnected] = useState(false);

  useEffect(() => {
    wsClient.connect();

    const handleRoomsUpdated = (updatedRooms: Room[]) => {
      setRooms(updatedRooms);
      setSelectedRoom((prev) => {
        if (!prev) return null;
        const found = updatedRooms.find((r) => r.id === prev.id);
        return found || null;
      });
    };

    const handleLogsUpdated = (updatedLogs: RoomLog[]) => {
      setLogs(updatedLogs);
    };

    const handleRoomSelected = (room: Room) => {
      setSelectedRoom(room);
    };

    const handleFilterChanged = (f: FilterState) => {
      setFilter(f);
    };

    const handleWsConnected = () => setWsConnected(true);
    const handleWsDisconnected = () => setWsConnected(false);

    eventBus.on('rooms:updated', handleRoomsUpdated);
    eventBus.on('logs:updated', handleLogsUpdated);
    eventBus.on(Events.ROOM_SELECTED, handleRoomSelected);
    eventBus.on(Events.FILTER_CHANGED, handleFilterChanged);
    eventBus.on(Events.WS_CONNECTED, handleWsConnected);
    eventBus.on(Events.WS_DISCONNECTED, handleWsDisconnected);

    return () => {
      eventBus.off('rooms:updated', handleRoomsUpdated);
      eventBus.off('logs:updated', handleLogsUpdated);
      eventBus.off(Events.ROOM_SELECTED, handleRoomSelected);
      eventBus.off(Events.FILTER_CHANGED, handleFilterChanged);
      eventBus.off(Events.WS_CONNECTED, handleWsConnected);
      eventBus.off(Events.WS_DISCONNECTED, handleWsDisconnected);
      wsClient.disconnect();
    };
  }, []);

  const floors = Array.from(new Set(rooms.map((r) => r.floor))).sort((a, b) => a - b);

  const filteredRooms = rooms.filter((room) => {
    if (filter.floor !== 'all' && room.floor !== filter.floor) return false;
    if (filter.status !== 'all' && room.status !== filter.status) return false;
    return true;
  });

  const todayCount = rooms.filter((r) => {
    if (!r.currentStay) return false;
    const today = new Date();
    const checkIn = new Date(r.currentStay.checkInTime);
    return (
      today.getFullYear() === checkIn.getFullYear() &&
      today.getMonth() === checkIn.getMonth() &&
      today.getDate() === checkIn.getDate()
    );
  }).length;

  return (
    <div className="room-status-page">
      <header className="app-header">
        <div className="hotel-name">悦享民宿 · 房态管理</div>
        <div className="header-stats">
          <div className="stat-item">
            <span className={`conn-dot ${wsConnected ? 'conn-online' : 'conn-offline'}`}></span>
            <span>{wsConnected ? '已连接' : '连接中断'}</span>
          </div>
          <div className="stat-item">
            <span className="stat-label">今日入住</span>
            <span className="stat-value">{todayCount}</span>
          </div>
          <div className="stat-item">
            <span className="stat-label">房间总数</span>
            <span className="stat-value">{rooms.length}</span>
          </div>
        </div>
      </header>

      <main className="app-main">
        <section className="main-content">
          <div className="filter-wrapper">
            <FilterPanel floors={floors} />
          </div>
          <div className="grid-wrapper">
            <RoomGridPanel rooms={filteredRooms} selectedRoomId={selectedRoom?.id || null} />
          </div>
        </section>

        <aside className="side-panel">
          <CheckInPanel selectedRoom={selectedRoom} />
        </aside>
      </main>

      <footer className="app-footer">
        <LogPanel logs={logs} />
      </footer>
    </div>
  );
}
