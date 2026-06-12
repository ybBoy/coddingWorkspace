/**
 * ToastContainer 消息提示容器
 * 职责：显示操作成功/失败/警告/信息的轻量提示，自动消失
 * 订阅 EventBus 的 SHOW_TOAST 事件
 */
import React, { useEffect, useState } from 'react';
import { eventBus, EVENTS } from './EventBus';
import { ToastMessage } from './types';

const ToastContainer: React.FC = () => {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  useEffect(() => {
    const unsubscribe = eventBus.on(EVENTS.SHOW_TOAST, (toast: ToastMessage) => {
      setToasts((prev) => [...prev, toast]);
      const duration = toast.duration ?? 3000;
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== toast.id));
      }, duration);
    });
    return unsubscribe;
  }, []);

  if (toasts.length === 0) return null;

  return (
    <div className="toast-container">
      {toasts.map((toast) => (
        <div key={toast.id} className={`toast toast-${toast.type}`}>
          <div className="toast-title">{toast.title}</div>
          {toast.message && <div className="toast-message">{toast.message}</div>}
        </div>
      ))}
    </div>
  );
};

export default ToastContainer;
