import React, { useState, useEffect } from 'react';
import { useWebSocket } from './hooks/useWebSocket';
import { eventBus } from './utils/EventBus';
import { AudiencePanel } from './components/AudiencePanel';
import { WallScreen } from './components/WallScreen';
import { ModeratorPanel } from './components/ModeratorPanel';
import './App.css';

type Mode = 'audience' | 'wall' | 'moderator';

const App: React.FC = () => {
  const [mode, setMode] = useState<Mode>('audience');
  const [connected, setConnected] = useState(false);
  useWebSocket();

  useEffect(() => {
    const unsub1 = eventBus.on('WS_CONNECTED', () => setConnected(true));
    const unsub2 = eventBus.on('WS_DISCONNECTED', () => setConnected(false));

    const hash = window.location.hash.replace('#', '');
    if (hash === 'wall') {
      setMode('wall');
    } else if (hash === 'moderator') {
      setMode('moderator');
    }

    return () => {
      unsub1();
      unsub2();
    };
  }, []);

  const handleModeChange = (newMode: Mode) => {
    setMode(newMode);
    window.location.hash = newMode === 'audience' ? '' : newMode;
    eventBus.emit('MODE_CHANGE', newMode);
  };

  const renderContent = () => {
    switch (mode) {
      case 'audience':
        return <AudiencePanel />;
      case 'wall':
        return <WallScreen />;
      case 'moderator':
        return <ModeratorPanel />;
      default:
        return <AudiencePanel />;
    }
  };

  return (
    <div className={`app mode-${mode}`}>
      {mode !== 'wall' && (
        <nav className="mode-switcher">
          <button
            className={`mode-btn ${mode === 'audience' ? 'active' : ''}`}
            onClick={() => handleModeChange('audience')}
          >
            📱 观众端
          </button>
          <button
            className={`mode-btn ${mode === 'wall' ? 'active' : ''}`}
            onClick={() => handleModeChange('wall')}
          >
            🖥️ 大屏
          </button>
          <button
            className={`mode-btn ${mode === 'moderator' ? 'active' : ''}`}
            onClick={() => handleModeChange('moderator')}
          >
            🎛️ 主持人
          </button>
          <span className={`nav-connection ${connected ? 'ok' : 'bad'}`}>
            {connected ? '●' : '○'}
          </span>
        </nav>
      )}

      <main className="main-content">
        {renderContent()}
      </main>
    </div>
  );
};

export default App;
