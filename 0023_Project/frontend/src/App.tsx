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

  const getModeBtnClass = (btnMode: Mode) => {
    return 'mode-btn' + (mode === btnMode ? ' active' : '');
  };

  return (
    <div className={'app mode-' + mode}>
      {mode !== 'wall' && (
        <nav className="mode-switcher">
          <button
            className={getModeBtnClass('audience')}
            onClick={() => handleModeChange('audience')}
          >
            Audience
          </button>
          <button
            className={getModeBtnClass('wall')}
            onClick={() => handleModeChange('wall')}
          >
            Wall Screen
          </button>
          <button
            className={getModeBtnClass('moderator')}
            onClick={() => handleModeChange('moderator')}
          >
            Moderator
          </button>
          <span className={'nav-connection ' + (connected ? 'ok' : 'bad')}>
            {connected ? 'ON' : 'OFF'}
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
