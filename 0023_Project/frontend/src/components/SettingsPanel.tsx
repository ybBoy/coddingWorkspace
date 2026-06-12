import React, { useState, useEffect } from 'react';
import { eventBus } from '../utils/EventBus';
import { Settings } from '../types';

interface Props {
  settings: Settings;
  token: string;
}

export const SettingsPanel: React.FC<Props> = ({ settings, token }) => {
  const [form, setForm] = useState<Settings>(settings);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    setForm(settings);
  }, [settings]);

  const handleSave = () => {
    eventBus.emit('UPDATE_SETTINGS', { data: form });
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const updateField = <K extends keyof Settings>(key: K, value: Settings[K]) => {
    setForm(prev => ({ ...prev, [key]: value }));
  };

  return (
    <div className="settings-panel">
      <h2>Activity Settings</h2>

      <div className="settings-section">
        <h3>Event Info</h3>
        <div className="form-group">
          <label>Event Title</label>
          <input
            type="text"
            value={form.eventTitle}
            onChange={e => updateField('eventTitle', e.target.value)}
            className="settings-input"
          />
        </div>
        <div className="form-group">
          <label>Welcome Message</label>
          <input
            type="text"
            value={form.welcomeMessage}
            onChange={e => updateField('welcomeMessage', e.target.value)}
            className="settings-input"
          />
        </div>
      </div>

      <div className="settings-section">
        <h3>Color Theme</h3>
        <select
          value={form.colorTheme}
          onChange={e => updateField('colorTheme', e.target.value)}
          className="settings-select"
        >
          <option value="rainbow">Rainbow</option>
          <option value="neon">Neon</option>
          <option value="warm">Warm</option>
          <option value="cool">Cool</option>
          <option value="custom">Custom</option>
        </select>
        {form.colorTheme === 'custom' && (
          <div className="form-group" style={{ marginTop: 8 }}>
            <label>Custom Colors (comma-separated hex)</label>
            <input
              type="text"
              value={(form.customColors || []).join(',')}
              onChange={e => updateField('customColors', e.target.value.split(',').map(s => s.trim()).filter(Boolean))}
              placeholder="#ff0000, #00ff00, #0000ff"
              className="settings-input"
            />
          </div>
        )}
      </div>

      <div className="settings-section">
        <h3>Danmaku Display</h3>
        <div className="settings-row">
          <div className="form-group">
            <label>Speed Min (s)</label>
            <input type="number" min={3} max={20} value={form.speedMin}
              onChange={e => updateField('speedMin', Number(e.target.value))} className="settings-input sm" />
          </div>
          <div className="form-group">
            <label>Speed Max (s)</label>
            <input type="number" min={5} max={30} value={form.speedMax}
              onChange={e => updateField('speedMax', Number(e.target.value))} className="settings-input sm" />
          </div>
        </div>
        <div className="settings-row">
          <div className="form-group">
            <label>Font Size (px)</label>
            <input type="number" min={14} max={60} value={form.fontSize}
              onChange={e => updateField('fontSize', Number(e.target.value))} className="settings-input sm" />
          </div>
          <div className="form-group">
            <label>Track Count</label>
            <input type="number" min={4} max={24} value={form.trackCount}
              onChange={e => updateField('trackCount', Number(e.target.value))} className="settings-input sm" />
          </div>
        </div>
      </div>

      <div className="settings-section">
        <h3>Sensitive Words</h3>
        <textarea
          value={(form.sensitiveWords || []).join('\n')}
          onChange={e => updateField('sensitiveWords', e.target.value.split('\n').map(s => s.trim()).filter(Boolean))}
          rows={4}
          className="settings-textarea"
        />
        <p className="hint">One word per line</p>
      </div>

      <div className="settings-section">
        <h3>Moderator Password</h3>
        <input
          type="password"
          value={form.moderatorPassword}
          onChange={e => updateField('moderatorPassword', e.target.value)}
          className="settings-input"
        />
      </div>

      <div className="settings-section">
        <h3>Data Management</h3>
        <div className="settings-row">
          <button className="control-btn primary" onClick={() => eventBus.emit('ROTATE_BACKUP')}>
            Rotate Backup
          </button>
          <button className="control-btn primary" onClick={() => eventBus.emit('EXPORT_DATA')}>
            Export Data
          </button>
        </div>
      </div>

      <button className="save-btn" onClick={handleSave}>
        {saved ? 'Saved!' : 'Save All Settings'}
      </button>
    </div>
  );
};
