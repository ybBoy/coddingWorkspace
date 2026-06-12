/**
 * CounterPanel 窗口操作面板组件
 * 职责：工作人员选择窗口、选择业务类型筛选、叫号、完成、过号、重新叫号
 * 通过 EventBus 发布操作事件
 * 迭代新增：按业务类型筛选叫号，支持窗口配置的业务类型
 * 数据流：工作人员操作 -> EventBus 事件 -> App.tsx -> WebSocket -> 后端
 */
import React, { useEffect, useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { QueueState, Counter, BusinessType, BUSINESS_TYPES } from './types';

const CounterPanel: React.FC = () => {
  const [state, setState] = useState<QueueState | null>(null);
  const [selectedCounterId, setSelectedCounterId] = useState<string | null>(null);
  const [selectedBusinessType, setSelectedBusinessType] = useState<BusinessType | 'all'>('all');

  useEffect(() => {
    const unsubscribe = eventBus.on(EVENTS.QUEUE_STATE_UPDATED, (newState: QueueState) => {
      setState(newState);
      const enabledCounters = newState.counters.filter((c) => c.enabled);
      if (!selectedCounterId && enabledCounters.length > 0) {
        setSelectedCounterId(enabledCounters[0].id);
      }
    });
    return unsubscribe;
  }, [selectedCounterId]);

  const selectedCounter = state?.counters.find((c) => c.id === selectedCounterId);
  const currentTicket = selectedCounter?.currentTicket;

  const availableBusinessTypes: (BusinessType | 'all')[] = ['all', ...(selectedCounter?.supportedBusinessTypes || BUSINESS_TYPES)];

  const hasWaiting = state && state.waitingQueue.length > 0;
  const hasWaitingByType = hasWaiting && (
    selectedBusinessType === 'all' ||
    state!.waitingQueue.some((t) => t.businessType === selectedBusinessType)
  );
  const hasCurrent = !!(selectedCounter && selectedCounter.currentTicket);

  const handleSelectCounter = (counterId: string) => {
    setSelectedCounterId(counterId);
    setSelectedBusinessType('all');
    eventBus.emit(EVENTS.SELECT_COUNTER, { counterId });
  };

  const handleCallNext = () => {
    if (!selectedCounterId) return;
    if (selectedBusinessType === 'all') {
      eventBus.emit(EVENTS.CALL_NEXT, { counterId: selectedCounterId });
    } else {
      eventBus.emit(EVENTS.CALL_NEXT_BY_TYPE, {
        counterId: selectedCounterId,
        businessType: selectedBusinessType,
      });
    }
  };

  const handleComplete = () => {
    if (selectedCounterId && currentTicket) {
      eventBus.emit(EVENTS.COMPLETE_TICKET, {
        counterId: selectedCounterId,
        ticketId: currentTicket.id,
      });
    }
  };

  const handleMiss = () => {
    if (selectedCounterId && currentTicket) {
      eventBus.emit(EVENTS.MISS_TICKET, {
        counterId: selectedCounterId,
        ticketId: currentTicket.id,
      });
    }
  };

  const handleRecall = () => {
    if (selectedCounterId && currentTicket) {
      eventBus.emit(EVENTS.RECALL_TICKET, {
        counterId: selectedCounterId,
        ticketId: currentTicket.id,
      });
    }
  };

  if (!state) {
    return null;
  }

  const enabledCounters = state.counters.filter((c) => c.enabled);

  return (
    <div className="counter-panel">
      <h2 className="section-title">窗口操作</h2>

      <div className="counter-selector">
        <label style={{ display: 'block', marginBottom: '8px', fontWeight: 500 }}>
          选择窗口
        </label>
        <div className="counter-list">
          {enabledCounters.map((counter: Counter) => (
            <div
              key={counter.id}
              className={`counter-item ${selectedCounterId === counter.id ? 'selected' : ''}`}
              onClick={() => handleSelectCounter(counter.id)}
            >
              <div>
                <span className="counter-name">{counter.name}</span>
                <div style={{ marginTop: 4 }}>
                  {counter.supportedBusinessTypes.map((t) => (
                    <span key={t} className="business-tag" style={{ marginRight: 4, fontSize: '0.7rem' }}>
                      {t}
                    </span>
                  ))}
                </div>
              </div>
              <span className={`counter-status ${counter.status}`}>
                {counter.status === 'idle' ? '空闲' : '忙碌'}
              </span>
            </div>
          ))}
        </div>
      </div>

      {selectedCounter && (
        <>
          <div className="form-group">
            <label>叫号业务类型</label>
            <select
              value={selectedBusinessType}
              onChange={(e) => setSelectedBusinessType(e.target.value as BusinessType | 'all')}
            >
              {availableBusinessTypes.map((t) => (
                <option key={t} value={t}>
                  {t === 'all' ? '全部业务' : t}
                </option>
              ))}
            </select>
          </div>

          {currentTicket ? (
            <div className="current-ticket-info">
              <div className="number">
                {String(currentTicket.number).padStart(3, '0')}
              </div>
              <div className="business">{currentTicket.businessType}</div>
            </div>
          ) : (
            <div className="current-ticket-info" style={{ color: 'var(--text-secondary)' }}>
              当前无办理业务
            </div>
          )}

          <button
            className="btn btn-primary"
            onClick={handleCallNext}
            disabled={!hasWaitingByType || hasCurrent}
          >
            {selectedBusinessType === 'all' ? '叫下一个号' : `叫下一个${selectedBusinessType}号`}
          </button>

          <div className="btn-group">
            <button
              className="btn btn-success"
              onClick={handleComplete}
              disabled={!hasCurrent}
            >
              完成
            </button>
            <button
              className="btn btn-warning"
              onClick={handleMiss}
              disabled={!hasCurrent}
            >
              过号
            </button>
          </div>

          <button
            className="btn btn-primary"
            onClick={handleRecall}
            disabled={!hasCurrent}
            style={{ marginTop: '8px' }}
          >
            重新叫号
          </button>
        </>
      )}
    </div>
  );
};

export default CounterPanel;
