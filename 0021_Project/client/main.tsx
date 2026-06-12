import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// 前端入口文件
// 负责渲染根组件 App
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
