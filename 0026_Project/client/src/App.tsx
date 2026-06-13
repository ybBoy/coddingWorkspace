import React, { useState } from 'react';
import GroupPage from './pages/GroupPage';
import BigScreenPage from './pages/BigScreenPage';

function App() {
  const [showBigScreen, setShowBigScreen] = useState(false);

  if (showBigScreen) {
    return <BigScreenPage onExit={() => setShowBigScreen(false)} />;
  }

  return <GroupPage onEnterBigScreen={() => setShowBigScreen(true)} />;
}

export default App;
