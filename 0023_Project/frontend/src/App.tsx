import React, { useState, useEffect } from 'react';
import { useWebSocket } from './hooks/useWebSocket';
import { eventBus } from './utils/EventBus';
import { Settings } from './types';
import { AudiencePanel } from './components/AudiencePanel';
import { WallScreen } from './components/WallScreen';
import { ModeratorPanel } from './components/ModeratorPanel';
import './App.css';

type Mode = 'audience' | 'wall' | 'moderator';

const App: React.FC = () => {
  const [mode, setMode] = useState<Mode>('audience');
  const [connected, setConnected] = useState(false);
  const [settings, setSettings] = useState<Settings | null>(null);
  const [onlineCount, setOnlineCount] = useState(0);
  useWebSocket();

  useEffect(() => {
    const unsubs = [
      eventBus.on('WS_CONNECTED', () => setConnected(true)),
      eventBus.on('WS_DISCONNECTED', () => setConnected(false)),
      eventBus.on('SETTING_UPDATED', (data: any) => setSettings(data as Settings)),
      eventBus.on('ONLINE_COUNT', (data: any) => setOnlineCount(data?.onlineCount || 0)),
    ];
    const hash = window.location.hash.replace('#', '');
    if (hash === 'wall') setMode('wall');
    else if (hash === 'moderator') setMode('moderator');
    return () => unsubs.forEach(u => u());
  }, []);

  const handleModeChange = (newMode: Mode) => {
    setMode(newMode);
    window.location.hash = newMode === 'audience' ? '' : newMode;
  };

  const getModeBtnClass = (btnMode: Mode) => 'mode-btn' + (mode === btnMode ? ' active' : '');

  const renderContent = () => {
    switch (mode) {
      case 'audience': return <AudiencePanel />;
      case 'wall': return <WallScreen />;
      case 'moderator': return <ModeratorPanel />;
      default: return <AudiencePanel />;
    }
  };

  const pendingCount = settings?.pendingCount || 0;

  return (
    <div className={'app mode-' + mode}>
      {mode !== 'wall' && (
        <>
          <nav className="mode-switcher">
            <button className={getModeBtnClass('audience')} onClick={() => handleModeChange('audience')}>Audience</button>
            <button className={getModeBtnClass('wall')} onClick={() => handleModeChange('wall')}>Wall Screen</button>
            <button className={getModeBtnClass('moderator')} onClick={() => handleModeChange('moderator')}>Moderator</button>
            <div className="nav-status">
              <span className={'nav-conn ' + (connected ? 'ok' : 'bad')}>{connected ? 'ON' : 'OFF'}</span>
              <span className="nav-online">{'Online: ' + onlineCount}</span>
              {pendingCount > 0 && <span className="nav-pending">{'Pending: ' + pendingCount}</span>}
              {settings && <span className={settings.sendingEnabled ? 'nav-send-on' : 'nav-send-off'}>
                {settings.sendingEnabled ? 'Send ON' : 'Send OFF'}
              </span>}
            </div>
          </nav>
          {!connected && <div className="reconnect-banner">Connection lost, reconnecting...</div>}
        </>
      )}
      <main className="main-content">{renderContent()}</main>
    </div>
  );
};

export default App;
