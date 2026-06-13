import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Pet, CareRecord, PetStatus, ReminderConfig, UserRole } from '../types';
import { eventBus } from '../core/EventBus';
import { petSocket } from '../core/socket';
import PetCard from '../widgets/PetCard';
import CareLogPanel from '../widgets/CareLogPanel';
import PetForm from '../widgets/PetForm';
import StatusFilter, { FilterValue } from '../widgets/StatusFilter';
import StaffSelector from '../widgets/StaffSelector';
import SearchBar from '../widgets/SearchBar';
import ReminderAlert from '../widgets/ReminderAlert';
import ShiftSummary from '../widgets/ShiftSummary';
import ExportButton from '../widgets/ExportButton';
import PetDetailPanel from '../widgets/PetDetailPanel';

const PetBoardPage: React.FC = () => {
  const [pets, setPets] = useState<Pet[]>([]);
  const [recentRecords, setRecentRecords] = useState<CareRecord[]>([]);
  const [lastCareTimeByPet, setLastCareTimeByPet] = useState<Record<string, string>>({});
  const [attentionPetIds, setAttentionPetIds] = useState<string[]>([]);
  const [reminderConfigs, setReminderConfigs] = useState<ReminderConfig[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [filter, setFilter] = useState<FilterValue>('ALL');
  const [staffName, setStaffName] = useState('');
  const [userRole, setUserRole] = useState<UserRole>('STAFF');
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'time' | 'name'>('time');
  const [detailPetId, setDetailPetId] = useState<string | null>(null);
  const [, forceUpdate] = useState(0);

  const isAdmin = userRole === 'ADMIN';

  useEffect(() => {
    const handleInit = (data: {
      pets: Pet[];
      recentRecords: CareRecord[];
      lastCareTimeByPet: Record<string, string>;
      attentionPetIds: string[];
      reminderConfigs: ReminderConfig[];
    }) => {
      setPets(data.pets);
      setRecentRecords(data.recentRecords);
      setLastCareTimeByPet(data.lastCareTimeByPet || {});
      setAttentionPetIds(data.attentionPetIds || []);
      setReminderConfigs(data.reminderConfigs || []);
    };

    const handlePetAdded = (pet: Pet) => {
      setPets((prev) => [pet, ...prev]);
    };

    const handleStatusUpdated = (data: { pet: Pet }) => {
      setPets((prev) => prev.map((p) => (p.id === data.pet.id ? data.pet : p)));
    };

    const handleCareRecordAdded = (record: CareRecord) => {
      setRecentRecords((prev) => [record, ...prev].slice(0, 10));
      setLastCareTimeByPet((prev) => ({ ...prev, [record.petId]: record.time }));
    };

    const handlePetUpdated = (pet: Pet) => {
      setPets((prev) => prev.map((p) => (p.id === pet.id ? pet : p)));
    };

    const handleCareRecordDeleted = (recordId: string) => {
      setRecentRecords((prev) => prev.filter((r) => r.id !== recordId));
    };

    const handleAttentionUpdate = (data: {
      attentionPetIds: string[];
      lastCareTimeByPet: Record<string, string>;
    }) => {
      setAttentionPetIds(data.attentionPetIds);
      setLastCareTimeByPet(data.lastCareTimeByPet);
    };

    const handleReminderConfigUpdated = (configs: ReminderConfig[]) => {
      setReminderConfigs(configs);
    };

    const handleFilterChanged = (newFilter: FilterValue) => {
      setFilter(newFilter);
    };

    const handleSearchChanged = (data: { searchTerm: string; sortBy: 'time' | 'name' }) => {
      setSearchTerm(data.searchTerm);
      setSortBy(data.sortBy);
    };

    const handleStaffChanged = (data: { staffName: string; role: UserRole }) => {
      setStaffName(data.staffName);
      setUserRole(data.role);
    };

    const handleSocketConnected = () => setIsConnected(true);
    const handleSocketDisconnected = () => setIsConnected(false);

    eventBus.on('initData', handleInit);
    eventBus.on('petAdded', handlePetAdded);
    eventBus.on('statusUpdated', handleStatusUpdated);
    eventBus.on('careRecordAdded', handleCareRecordAdded);
    eventBus.on('petUpdated', handlePetUpdated);
    eventBus.on('careRecordDeleted', handleCareRecordDeleted);
    eventBus.on('attentionUpdate', handleAttentionUpdate);
    eventBus.on('reminderConfigUpdated', handleReminderConfigUpdated);
    eventBus.on('filterChanged', handleFilterChanged);
    eventBus.on('searchChanged', handleSearchChanged);
    eventBus.on('staffChanged', handleStaffChanged);
    eventBus.on('socketConnected', handleSocketConnected);
    eventBus.on('socketDisconnected', handleSocketDisconnected);

    petSocket.connect();

    const interval = setInterval(() => forceUpdate((n) => n + 1), 60000);

    return () => {
      eventBus.off('initData', handleInit);
      eventBus.off('petAdded', handlePetAdded);
      eventBus.off('statusUpdated', handleStatusUpdated);
      eventBus.off('careRecordAdded', handleCareRecordAdded);
      eventBus.off('petUpdated', handlePetUpdated);
      eventBus.off('careRecordDeleted', handleCareRecordDeleted);
      eventBus.off('attentionUpdate', handleAttentionUpdate);
      eventBus.off('reminderConfigUpdated', handleReminderConfigUpdated);
      eventBus.off('filterChanged', handleFilterChanged);
      eventBus.off('searchChanged', handleSearchChanged);
      eventBus.off('staffChanged', handleStaffChanged);
      eventBus.off('socketConnected', handleSocketConnected);
      eventBus.off('socketDisconnected', handleSocketDisconnected);
      petSocket.disconnect();
      clearInterval(interval);
    };
  }, []);

  const filteredPets = useMemo(() => {
    let result = pets;
    if (filter !== 'ALL') {
      result = result.filter((p) => p.status === filter);
    }
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      result = result.filter(
        (p) => p.name.toLowerCase().includes(term) || p.breed.toLowerCase().includes(term)
      );
    }
    if (sortBy === 'name') {
      result = [...result].sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'));
    }
    return result;
  }, [pets, filter, searchTerm, sortBy]);

  const todayCount = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return pets.filter((p) => new Date(p.checkInTime) >= today).length;
  }, [pets]);

  const getPetLastCareTime = useCallback(
    (petId: string): string | null => lastCareTimeByPet[petId] || null,
    [lastCareTimeByPet]
  );

  const checkNeedsAttention = useCallback(
    (pet: Pet): boolean => attentionPetIds.includes(pet.id),
    [attentionPetIds]
  );

  const handleOpenDetail = useCallback((petId: string) => {
    setDetailPetId(petId);
  }, []);

  const handleCloseDetail = useCallback(() => {
    setDetailPetId(null);
  }, []);

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
          <ReminderAlert
            attentionPetIds={attentionPetIds}
            reminderConfigs={reminderConfigs}
            pets={pets}
          />
          <div className={`connection-status ${isConnected ? 'connected' : 'disconnected'}`}>
            <span className="status-dot"></span>
            <span className="status-text">{isConnected ? '已连接' : '连接中...'}</span>
          </div>
          <div className="today-count">
            <span className="count-number">{todayCount}</span>
            <span className="count-label">今日入住</span>
          </div>
          <StaffSelector />
        </div>
      </header>

      <main className="app-main">
        <section className="pets-section">
          <div className="section-header">
            <h2>在店宠物</h2>
            <div className="section-header-actions">
              <SearchBar />
              <span className="pet-count">共 {filteredPets.length} 只</span>
            </div>
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
                  staffName={staffName}
                  isAdmin={isAdmin}
                  onOpenDetail={handleOpenDetail}
                />
              ))}
            </div>
          )}
        </section>

        <aside className="side-panel">
          <PetForm />
          <CareLogPanel records={recentRecords} />
          <ExportButton records={recentRecords} />
          <ShiftSummary />
        </aside>
      </main>

      <footer className="app-footer">
        <StatusFilter currentFilter={filter} />
      </footer>

      {detailPetId && (
        <PetDetailPanel
          petId={detailPetId}
          onClose={handleCloseDetail}
          isAdmin={isAdmin}
          staffName={staffName}
        />
      )}
    </div>
  );
};

export default PetBoardPage;
