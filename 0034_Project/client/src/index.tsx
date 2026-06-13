import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import PlantDashboard from './screens/PlantDashboard';

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);
root.render(
  <React.StrictMode>
    <PlantDashboard />
  </React.StrictMode>
);
