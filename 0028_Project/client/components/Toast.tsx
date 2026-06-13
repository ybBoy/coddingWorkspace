import React, { useEffect, useState } from 'react';
import { eventBus } from '../core/EventBus';

interface ToastItem {
  id: number;
  message: string;
  type: 'info' | 'success' | 'warning' | 'error';
}

let idCounter = 0;

const Toast: React.FC = () => {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  useEffect(() => {
    const unsub = eventBus.on<{ message: string; type?: string; duration?: number }>(
      'toast:show',
      (data) => {
        const id = ++idCounter;
        const type = (data.type as any) || 'info';
        const duration = data.duration || 3000;
        setToasts((prev) => [...prev, { id, message: data.message, type }]);
        setTimeout(() => {
          setToasts((prev) => prev.filter((t) => t.id !== id));
        }, duration);
      }
    );
    return unsub;
  }, []);

  return (
    <div className="toast-container">
      {toasts.map((t) => (
        <div key={t.id} className={`toast toast-${t.type}`}>
          {t.message}
        </div>
      ))}
    </div>
  );
};

export default Toast;
