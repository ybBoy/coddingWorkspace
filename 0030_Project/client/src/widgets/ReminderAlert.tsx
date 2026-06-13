import React, { useState, useRef } from 'react';
import { Pet, ReminderConfig, CARE_ACTIONS, STATUS_LABELS } from '../types';
import { petSocket } from '../core/socket';

interface ReminderAlertProps {
  attentionPetIds: string[];
  reminderConfigs: ReminderConfig[];
  pets: Pet[];
}

const ReminderAlert: React.FC<ReminderAlertProps> = ({ attentionPetIds, reminderConfigs, pets }) => {
  const [showPanel, setShowPanel] = useState(false);
  const [soundEnabled, setSoundEnabled] = useState(() => localStorage.getItem('petboard_sound') !== 'off');
  const audioCtxRef = useRef<AudioContext | null>(null);

  const count = attentionPetIds.length;

  const playBeep = () => {
    if (!soundEnabled) return;
    try {
      if (!audioCtxRef.current) {
        audioCtxRef.current = new AudioContext();
      }
      const ctx = audioCtxRef.current;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.frequency.value = 800;
      gain.gain.value = 0.15;
      osc.start();
      osc.stop(ctx.currentTime + 0.15);
    } catch (_) {}
  };

  React.useEffect(() => {
    if (count > 0) playBeep();
  }, [count]);

  const toggleSound = () => {
    const next = !soundEnabled;
    setSoundEnabled(next);
    localStorage.setItem('petboard_sound', next ? 'on' : 'off');
  };

  const getActionLabel = (action: string) => {
    const found = CARE_ACTIONS.find((a) => a.value === action);
    return found ? found.label : action;
  };

  const handleConfigChange = (action: string, field: 'intervalMinutes' | 'enabled', value: number | boolean) => {
    const existing = reminderConfigs.find((c) => c.action === action);
    const intervalMinutes = field === 'intervalMinutes' ? (value as number) : (existing ? existing.intervalMinutes : 360);
    const enabled = field === 'enabled' ? (value as boolean) : (existing ? existing.enabled : true);
    petSocket.setReminderConfig(action, intervalMinutes, enabled);
  };

  return (
    <div className="reminder-alert">
      <button className={`alert-btn ${count > 0 ? 'has-alert' : ''}`} onClick={() => setShowPanel(!showPanel)}>
        🔔
        {count > 0 && <span className="alert-badge">{count}</span>}
      </button>

      {showPanel && (
        <div className="alert-dropdown">
          <div className="alert-dropdown-header">
            <h4>提醒设置</h4>
            <button className="btn-icon" onClick={toggleSound} title={soundEnabled ? '关闭声音' : '开启声音'}>
              {soundEnabled ? '🔊' : '🔇'}
            </button>
          </div>

          {count > 0 && (
            <div className="alert-pets">
              <div className="alert-section-title">待关注宠物 ({count})</div>
              {attentionPetIds.map((id) => {
                const pet = pets.find((p) => p.id === id);
                if (!pet) return null;
                return (
                  <div key={id} className="alert-pet-item">
                    <span className="alert-pet-name">{pet.name}</span>
                    <span className="alert-pet-status">{STATUS_LABELS[pet.status]}</span>
                  </div>
                );
              })}
            </div>
          )}

          <div className="alert-section-title">提醒周期</div>
          {reminderConfigs.map((config) => (
            <div key={config.action} className="reminder-config-row">
              <label className="reminder-toggle">
                <input
                  type="checkbox"
                  checked={config.enabled}
                  onChange={(e) => handleConfigChange(config.action, 'enabled', e.target.checked)}
                />
                <span>{getActionLabel(config.action)}</span>
              </label>
              {config.enabled && (
                <div className="reminder-interval">
                  <input
                    type="number"
                    className="reminder-input"
                    value={config.intervalMinutes}
                    min={30}
                    step={30}
                    onChange={(e) => handleConfigChange(config.action, 'intervalMinutes', parseInt(e.target.value) || 360)}
                  />
                  <span className="reminder-unit">分钟</span>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default ReminderAlert;
