import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { eventBus, SeatData, SeatActionData, ZoneType } from '../core/EventBus';
import { initSocket } from '../core/socket';
import SeatGrid from '../components/SeatGrid';
import SeatInfoPanel from '../components/SeatInfoPanel';
import ActivityFeed from '../components/ActivityFeed';
import Toast from '../components/Toast';
import BroadcastBanner from '../components/BroadcastBanner';
import ZoneFilter from '../components/ZoneFilter';

const NICKNAME_KEY = 'studyroom_nickname';

const SeatMapPage: React.FC = () => {
  const [seats, setSeats] = useState<SeatData[]>([]);
  const [actions, setActions] = useState<SeatActionData[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [nickname, setNickname] = useState<string>(() => localStorage.getItem(NICKNAME_KEY) || '');
  const [connected, setConnected] = useState<boolean>(false);
  const [mySeatId, setMySeatId] = useState<number | null>(null);
  const [activeZone, setActiveZone] = useState<ZoneType | 'all'>('all');
  const [wasDisconnected, setWasDisconnected] = useState(false);

  const handleInit = useCallback((data: { seats: SeatData[]; actions: SeatActionData[] }) => {
    setSeats(data.seats);
    setActions(data.actions);
  }, []);

  const handleUpdate = useCallback((data: { seats: SeatData[]; actions: SeatActionData[] }) => {
    setSeats(data.seats);
    setActions(data.actions);
  }, []);

  const handleConnected = useCallback(() => {
    setConnected(true);
    if (wasDisconnected) {
      eventBus.emit('toast:show', { message: '已重新连接', type: 'success', duration: 2000 });
      setWasDisconnected(false);
    }
  }, [wasDisconnected]);

  const handleDisconnected = useCallback(() => {
    setConnected(false);
    setWasDisconnected(true);
  }, []);

  useEffect(() => {
    const unsubInit = eventBus.on('state:init', handleInit);
    const unsubUpdate = eventBus.on('state:update', handleUpdate);
    const unsubConn = eventBus.on('ws:connected', handleConnected);
    const unsubDisconn = eventBus.on('ws:disconnected', handleDisconnected);

    initSocket();

    return () => {
      unsubInit();
      unsubUpdate();
      unsubConn();
      unsubDisconn();
    };
  }, [handleInit, handleUpdate, handleConnected, handleDisconnected]);

  useEffect(() => {
    if (nickname) {
      localStorage.setItem(NICKNAME_KEY, nickname);
    }
  }, [nickname]);

  useEffect(() => {
    if (seats.length > 0 && nickname) {
      const found = seats.find(
        (s) => (s.status === 'occupied' || s.status === 'away') && s.nickname === nickname
      );
      setMySeatId(found ? found.id : null);
    } else {
      setMySeatId(null);
    }
  }, [seats, nickname]);

  const selectedSeat = selectedId !== null ? seats.find((s) => s.id === selectedId) || null : null;

  return (
    <div className="seat-map-page">
      <Toast />
      <BroadcastBanner />
      <header className="app-header">
        <div className="header-left">
          <h1 className="room-title">📚 自习室座位看板</h1>
        </div>
        <div className="header-center">
          <span className={`conn-status ${connected ? 'online' : 'offline'}`}>
            {connected ? '● 已连接' : '○ 未连接'}
          </span>
          {!connected && (
            <span className="reconnect-hint">正在重连...</span>
          )}
        </div>
        <div className="header-right">
          <label className="nickname-label">昵称：</label>
          <input
            className="nickname-input"
            type="text"
            placeholder="输入昵称"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            maxLength={10}
          />
          {mySeatId !== null && (() => {
            const mySeat = seats.find((s) => s.id === mySeatId);
            return mySeat ? (
              <span className="my-seat-badge">
                我的座位: {mySeat.row + 1}排{mySeat.col + 1}座
              </span>
            ) : null;
          })()}
          <Link to="/admin" className="admin-link">管理员</Link>
        </div>
      </header>

      <div className="page-subheader">
        <ZoneFilter activeZone={activeZone} onChange={setActiveZone} />
      </div>

      <div className="main-content">
        <div className="left-panel">
          <SeatGrid
            seats={seats}
            selectedId={selectedId}
            onSelect={setSelectedId}
            mySeatId={mySeatId}
            activeZone={activeZone}
          />
          <ActivityFeed actions={actions} />
        </div>
        <div className="right-panel">
          <SeatInfoPanel
            seat={selectedSeat}
            nickname={nickname}
            mySeatId={mySeatId}
            isAdmin={false}
          />
        </div>
      </div>
    </div>
  );
};

export default SeatMapPage;
