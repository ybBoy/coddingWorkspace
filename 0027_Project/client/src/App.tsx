import { useState } from 'react'
import BoothPage from './pages/BoothPage'
import DashboardPage from './pages/DashboardPage'

type PageType = 'booth' | 'dashboard'

function App() {
  const [currentPage, setCurrentPage] = useState<PageType>('booth')

  return (
    <div className="app-container">
      <nav className="app-header">
        <button
          className={`nav-btn ${currentPage === 'booth' ? 'active' : ''}`}
          onClick={() => setCurrentPage('booth')}
        >
          展位签到
        </button>
        <button
          className={`nav-btn ${currentPage === 'dashboard' ? 'active' : ''}`}
          onClick={() => setCurrentPage('dashboard')}
        >
          主办方看板
        </button>
      </nav>

      {currentPage === 'booth' ? <BoothPage /> : <DashboardPage />}
    </div>
  )
}

export default App
