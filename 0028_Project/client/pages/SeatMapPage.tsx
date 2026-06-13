import React, { useEffect, useState, useCallback } from 'react';
import { eventBus, SeatData, SeatActionData } from '../core/EventBus';
import { initSocket, sendAction, getConnected } from '../core/socket';
import SeatGrid from '../components/SeatGrid';
import SeatInfoPanel from '../components/SeatInfoPanel';
import ActivityFeed from '../components/ActivityFeed';

const NICKNAME_KEY = 'studyroom_nickname';

const SeatMapPage: React.FC = () => {
  const [seats, setSeats] = useState<SeatData[]>([]);
  const [actions, setActions] = useState<SeatActionData[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [nickname, setNickname] = useState<string>(() => localStorage.getItem(NICKNAME_KEY) || '');
  const [connected, setConnected] = useState<boolean>(false);
  const [mySeatId, setMySeatId] = useState<number | null>(null);

  const handleInit = useCallback((data: { seats: SeatData[]; actions: SeatActionData[] }) => {
    setSeats(data.seats);
    setActions(data.actions);
  }, []);

  const handleUpdate = useCallback((data: { seats: SeatData[]; actions: SeatActionData[] }) => {
    setSeats(data.seats);
    setActions(data.actions);
  }, []);

  const handleConnected = useCallback(() => setConnected(true), []);
  const handleDisconnected = useCallback(() => setConnected(false), []);

  const handleError = useCallback((msg: string) => {
    alert(msg);
  }, []);

  useEffect(() => {
    const unsubInit = eventBus.on('state:init', handleInit);
    const unsubUpdate = eventBus.on('state:update', handleUpdate);
    const unsubConn = eventBus.on('ws:connected', handleConnected);
    const unsubDisconn = eventBus.on('ws:disconnected', handleDisconnected);
    const unsubErr = eventBus.on('ws:error', handleError);

    initSocket();

    return () => {
      unsubInit();
      unsubUpdate();
      unsubConn();
      unsubDisconn();
      unsubErr();
    };
  }, [handleInit, handleUpdate, handleConnected, handleDisconnected, handleError]);

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
    }
  }, [seats, nickname]);

  const selectedSeat = selectedId !== null ? seats.find((s) => s.id === selectedId) || null : null;

  const handleNicknameConfirm = () => {
    if (!nickname.trim()) {
      alert('请输入昵称');
    }
  };

  return (
    <div className="seat-map-page">
      <header className="app-header">
        <div className="header-left">
          <h1 className="room-title">📚 自习室座位看板</h1>
        </div>
        <div className="header-center">
          <span className={`conn-status ${connected ? 'online' : 'offline'}`}>
            {connected ? '● 已连接' : '○ 未连接'}
          </span>
        </div>
        <div className="header-right">
          <label className="nickname-label">昵称：</label>
          <input
            className="nickname-input"
            type="text"
            placeholder="输入昵称"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            onBlur={handleNicknameConfirm}
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
        </div>
      </header>

      <div className="main-content">
        <div className="left-panel">
          <SeatGrid
            seats={seats}
            selectedId={selectedId}
            onSelect={setSelectedId}
            mySeatId={mySeatId}
          />
          <ActivityFeed actions={actions} />
        </div>
        <div className="right-panel">
          <SeatInfoPanel
            seat={selectedSeat}
            nickname={nickname}
            mySeatId={mySeatId}
          />
        </div>
      </div>
    </div>
  );
};

export default SeatMapPage;
