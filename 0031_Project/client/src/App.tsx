import { useState } from 'react';
import { RoomStatusPage } from './pages/RoomStatusPage';
import { AdminPage } from './pages/AdminPage';
import type { PageType } from './base/types';

function App() {
  const [currentPage, setCurrentPage] = useState<PageType>('status');

  return (
    <div className="app-root">
      <nav className="app-nav">
        <button
          className={`nav-btn ${currentPage === 'status' ? 'active' : ''}`}
          onClick={() => setCurrentPage('status')}
        >
          🏨 房态板
        </button>
        <button
          className={`nav-btn ${currentPage === 'admin' ? 'active' : ''}`}
          onClick={() => setCurrentPage('admin')}
        >
          ⚙️ 管理
        </button>
      </nav>
      <div className="app-content">
        {currentPage === 'status' && <RoomStatusPage />}
        {currentPage === 'admin' && <AdminPage />}
      </div>
    </div>
  );
}

export default App;
