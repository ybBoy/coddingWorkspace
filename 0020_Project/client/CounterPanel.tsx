/**
 * CounterPanel 窗口操作面板组件
 * 职责：工作人员选择窗口、叫号、完成、过号、重新叫号
 * 通过 EventBus 发布操作事件
 * 数据流：工作人员操作 -> EventBus 事件 -> App.tsx -> WebSocket -> 后端
 */
import React, { useEffect, useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { QueueState, Counter } from './types';

const CounterPanel: React.FC = () => {
  const [state, setState] = useState<QueueState | null>(null);
  const [selectedCounterId, setSelectedCounterId] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribe = eventBus.on(EVENTS.QUEUE_STATE_UPDATED, (newState: QueueState) => {
      setState(newState);
      if (!selectedCounterId && newState.counters.length > 0) {
        setSelectedCounterId(newState.counters[0].id);
      }
    });
    return unsubscribe;
  }, [selectedCounterId]);

  const selectedCounter = state?.counters.find((c) => c.id === selectedCounterId);
  const currentTicket = selectedCounter?.currentTicket;
  const hasWaiting = state && state.waitingQueue.length > 0;
  const hasCurrent = selectedCounter && selectedCounter.currentTicket;

  const handleSelectCounter = (counterId: string) => {
    setSelectedCounterId(counterId);
    eventBus.emit(EVENTS.SELECT_COUNTER, { counterId });
  };

  const handleCallNext = () => {
    if (selectedCounterId) {
      eventBus.emit(EVENTS.CALL_NEXT, { counterId: selectedCounterId });
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

  return (
    <div className="counter-panel">
      <h2 className="section-title">窗口操作</h2>

      <div className="counter-selector">
        <label style={{ display: 'block', marginBottom: '8px', fontWeight: 500 }}>
          选择窗口
        </label>
        <div className="counter-list">
          {state.counters.map((counter: Counter) => (
            <div
              key={counter.id}
              className={`counter-item ${selectedCounterId === counter.id ? 'selected' : ''}`}
              onClick={() => handleSelectCounter(counter.id)}
            >
              <span className="counter-name">{counter.name}</span>
              <span className={`counter-status ${counter.status}`}>
                {counter.status === 'idle' ? '空闲' : '忙碌'}
              </span>
            </div>
          ))}
        </div>
      </div>

      {selectedCounter && (
        <>
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
            disabled={!hasWaiting || hasCurrent}
          >
            叫下一个号
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
