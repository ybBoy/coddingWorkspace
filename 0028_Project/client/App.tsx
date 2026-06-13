import React from 'react';
import { HashRouter, Routes, Route } from 'react-router-dom';
import SeatMapPage from './pages/SeatMapPage';
import AdminPage from './pages/AdminPage';
import './styles.css';

const App: React.FC = () => {
  return (
    <HashRouter>
      <Routes>
        <Route path="/" element={<SeatMapPage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Routes>
    </HashRouter>
  );
};

export default App;
