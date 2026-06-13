import React, { useEffect, useMemo, useState } from 'react';
import { Pet, CareRecord, PetStatus } from '../types';
import { eventBus } from '../core/EventBus';
import { petSocket } from '../core/socket';
import PetCard from '../widgets/PetCard';
import CareLogPanel from '../widgets/CareLogPanel';
import PetForm from '../widgets/PetForm';
import StatusFilter, { FilterValue } from '../widgets/StatusFilter';

const PetBoardPage: React.FC = () => {
  const [pets, setPets] = useState<Pet[]>([]);
  const [recentRecords, setRecentRecords] = useState<CareRecord[]>([]);
  const [lastCareTimeByPet, setLastCareTimeByPet] = useState<Record<string, string>>({});
  const [isConnected, setIsConnected] = useState(false);
  const [filter, setFilter] = useState<FilterValue>('ALL');
  const [, forceUpdate] = useState(0);

  useEffect(() => {
    const handleInit = (data: { pets: Pet[]; recentRecords: CareRecord[]; lastCareTimeByPet: Record<string, string> }) => {
      setPets(data.pets);
      setRecentRecords(data.recentRecords);
      setLastCareTimeByPet(data.lastCareTimeByPet || {});
    };

    const handlePetAdded = (pet: Pet) => {
      setPets((prev) => [pet, ...prev]);
    };

    const handleStatusUpdated = (updatedPet: Pet) => {
      setPets((prev) =>
        prev.map((p) => (p.id === updatedPet.id ? updatedPet : p))
      );
    };

    const handleCareRecordAdded = (record: CareRecord) => {
      setRecentRecords((prev) => [record, ...prev].slice(0, 10));
      setLastCareTimeByPet((prev) => ({
        ...prev,
        [record.petId]: record.time,
      }));
    };

    const handleFilterChanged = (newFilter: FilterValue) => {
      setFilter(newFilter);
    };

    const handleSocketConnected = () => {
      setIsConnected(true);
    };

    const handleSocketDisconnected = () => {
      setIsConnected(false);
    };

    eventBus.on('initData', handleInit);
    eventBus.on('petAdded', handlePetAdded);
    eventBus.on('statusUpdated', handleStatusUpdated);
    eventBus.on('careRecordAdded', handleCareRecordAdded);
    eventBus.on('filterChanged', handleFilterChanged);
    eventBus.on('socketConnected', handleSocketConnected);
    eventBus.on('socketDisconnected', handleSocketDisconnected);

    petSocket.connect();

    const interval = setInterval(() => {
      forceUpdate((n) => n + 1);
    }, 60000);

    return () => {
      eventBus.off('initData', handleInit);
      eventBus.off('petAdded', handlePetAdded);
      eventBus.off('statusUpdated', handleStatusUpdated);
      eventBus.off('careRecordAdded', handleCareRecordAdded);
      eventBus.off('filterChanged', handleFilterChanged);
      eventBus.off('socketConnected', handleSocketConnected);
      eventBus.off('socketDisconnected', handleSocketDisconnected);
      petSocket.disconnect();
      clearInterval(interval);
    };
  }, []);

  const filteredPets = useMemo(() => {
    if (filter === 'ALL') return pets;
    return pets.filter((p) => p.status === filter);
  }, [pets, filter]);

  const todayCount = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return pets.filter((p) => new Date(p.checkInTime) >= today).length;
  }, [pets]);

  const getPetLastCareTime = (petId: string): string | null => {
    return lastCareTimeByPet[petId] || null;
  };

  const checkNeedsAttention = (pet: Pet): boolean => {
    if (pet.status === 'PICKED_UP') return false;
    const lastCareTime = getPetLastCareTime(pet.id);
    if (!lastCareTime) {
      const checkIn = new Date(pet.checkInTime).getTime();
      const now = Date.now();
      return now - checkIn > 6 * 60 * 60 * 1000;
    }
    const last = new Date(lastCareTime).getTime();
    const now = Date.now();
    return now - last > 6 * 60 * 60 * 1000;
  };

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-left">
          <div className="shop-logo">🐾</div>
          <div className="shop-info">
            <h1 className="shop-name">萌宠乐园寄养中心</h1>
            <div className="shop-subtitle">今日温馨陪伴</div>
          </div>
        </div>
        <div className="header-right">
          <div className={`connection-status ${isConnected ? 'connected' : 'disconnected'}`}>
            <span className="status-dot"></span>
            <span className="status-text">{isConnected ? '已连接' : '连接中...'}</span>
          </div>
          <div className="today-count">
            <span className="count-number">{todayCount}</span>
            <span className="count-label">今日入住</span>
          </div>
        </div>
      </header>

      <main className="app-main">
        <section className="pets-section">
          <div className="section-header">
            <h2>在店宠物</h2>
            <span className="pet-count">共 {filteredPets.length} 只</span>
          </div>

          {filteredPets.length === 0 ? (
            <div className="empty-pets">
              <div className="empty-pets-icon">🐶🐱</div>
              <div className="empty-pets-text">暂无符合条件的宠物</div>
            </div>
          ) : (
            <div className="pets-grid">
              {filteredPets.map((pet) => (
                <PetCard
                  key={pet.id}
                  pet={pet}
                  lastCareTime={getPetLastCareTime(pet.id)}
                  needsAttention={checkNeedsAttention(pet)}
                />
              ))}
            </div>
          )}
        </section>

        <aside className="side-panel">
          <PetForm />
          <CareLogPanel records={recentRecords} />
        </aside>
      </main>

      <footer className="app-footer">
        <StatusFilter currentFilter={filter} />
      </footer>
    </div>
  );
};

export default PetBoardPage;
