import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './styles.css'

// 前端入口
// 只做一件事：把 App 根组件挂到 #root
// WebSocket 连接、EventBus 初始化全部由 App.tsx 内部的 useEffect 触发
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
