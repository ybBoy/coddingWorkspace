import { useEffect, useState } from 'react';
import { eventBus, Events } from '../base/EventBus';
import { wsClient } from '../base/wsClient';
import { FilterPanel } from '../panels/FilterPanel';
import { RoomGridPanel } from '../panels/RoomGridPanel';
import { CheckInPanel } from '../panels/CheckInPanel';
import { LogPanel } from '../panels/LogPanel';
import { AlertPanel } from '../panels/AlertPanel';
import { BatchPanel } from '../panels/BatchPanel';
import { ExportPanel } from '../panels/ExportPanel';
import { RoomDetailPanel } from '../panels/RoomDetailPanel';
import type { Room, RoomLog, RoomStatus, Operator, AlertItem } from '../base/types';
import './RoomStatusPage.css';

interface FilterState {
  floor: number | 'all';
  status: RoomStatus | 'all';
}

type SidePanelTab = 'action' | 'detail' | 'batch' | 'export';

export function RoomStatusPage() {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [logs, setLogs] = useState<RoomLog[]>([]);
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<Room | null>(null);
  const [filter, setFilter] = useState<FilterState>({ floor: 'all', status: 'all' });
  const [wsConnected, setWsConnected] = useState(false);
  const [currentOperator, setCurrentOperator] = useState('前台');
  const [operators, setOperators] = useState<Operator[]>([]);
  const [sideTab, setSideTab] = useState<SidePanelTab>('action');

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

    const handleAlertsUpdated = (updatedAlerts: AlertItem[]) => {
      setAlerts(updatedAlerts);
    };

    const handleRoomSelected = (room: Room) => {
      setSelectedRoom(room);
      setSideTab('action');
    };

    const handleFilterChanged = (f: FilterState) => {
      setFilter(f);
    };

    const handleWsConnected = () => setWsConnected(true);
    const handleWsDisconnected = () => setWsConnected(false);

    const handleOperatorsUpdated = (ops: Operator[]) => setOperators(ops);
    const handleOperatorChanged = (name: string) => setCurrentOperator(name);

    eventBus.on(Events.ROOMS_UPDATED, handleRoomsUpdated);
    eventBus.on('logs:updated', handleLogsUpdated);
    eventBus.on(Events.ALERTS_UPDATED, handleAlertsUpdated);
    eventBus.on(Events.ROOM_SELECTED, handleRoomSelected);
    eventBus.on(Events.FILTER_CHANGED, handleFilterChanged);
    eventBus.on(Events.WS_CONNECTED, handleWsConnected);
    eventBus.on(Events.WS_DISCONNECTED, handleWsDisconnected);
    eventBus.on(Events.OPERATORS_UPDATED, handleOperatorsUpdated);
    eventBus.on(Events.OPERATOR_CHANGED, handleOperatorChanged);

    return () => {
      eventBus.off(Events.ROOMS_UPDATED, handleRoomsUpdated);
      eventBus.off('logs:updated', handleLogsUpdated);
      eventBus.off(Events.ALERTS_UPDATED, handleAlertsUpdated);
      eventBus.off(Events.ROOM_SELECTED, handleRoomSelected);
      eventBus.off(Events.FILTER_CHANGED, handleFilterChanged);
      eventBus.off(Events.WS_CONNECTED, handleWsConnected);
      eventBus.off(Events.WS_DISCONNECTED, handleWsDisconnected);
      eventBus.off(Events.OPERATORS_UPDATED, handleOperatorsUpdated);
      eventBus.off(Events.OPERATOR_CHANGED, handleOperatorChanged);
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

  const handleOperatorChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const name = e.target.value;
    setCurrentOperator(name);
    wsClient.setCurrentOperator(name);
  };

  return (
    <div className="room-status-page">
      <header className="app-header">
        <div className="header-left">
          <div className="hotel-name">悦享民宿 · 房态管理</div>
        </div>
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
          {alerts.length > 0 && (
            <div className="stat-item alert-stat">
              <span className="stat-label">⚠️ 异常</span>
              <span className="stat-value alert-value">{alerts.length}</span>
            </div>
          )}
          <div className="stat-item operator-item">
            <span className="stat-label">操作人</span>
            <select className="operator-select" value={currentOperator} onChange={handleOperatorChange}>
              {operators.map((op) => (
                <option key={op.id} value={op.name}>
                  {op.name}
                </option>
              ))}
            </select>
          </div>
        </div>
      </header>

      {alerts.length > 0 && (
        <div className="alert-bar">
          <AlertPanel />
        </div>
      )}

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
          <div className="side-tabs">
            <button
              className={`side-tab ${sideTab === 'action' ? 'active' : ''}`}
              onClick={() => setSideTab('action')}
            >
              📋 操作
            </button>
            <button
              className={`side-tab ${sideTab === 'detail' ? 'active' : ''}`}
              onClick={() => setSideTab('detail')}
            >
              📄 详情
            </button>
            <button
              className={`side-tab ${sideTab === 'batch' ? 'active' : ''}`}
              onClick={() => setSideTab('batch')}
            >
              📦 批量
            </button>
            <button
              className={`side-tab ${sideTab === 'export' ? 'active' : ''}`}
              onClick={() => setSideTab('export')}
            >
              📊 导出
            </button>
          </div>
          <div className="side-content">
            {sideTab === 'action' && <CheckInPanel selectedRoom={selectedRoom} />}
            {sideTab === 'detail' && <RoomDetailPanel roomId={selectedRoom?.id || null} />}
            {sideTab === 'batch' && <BatchPanel rooms={rooms} />}
            {sideTab === 'export' && <ExportPanel />}
          </div>
        </aside>
      </main>

      <footer className="app-footer">
        <LogPanel logs={logs} />
      </footer>
    </div>
  );
}
