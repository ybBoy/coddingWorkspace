import React, { useEffect, useState } from 'react';
import { eventBus } from '../core/EventBus';

const BroadcastBanner: React.FC = () => {
  const [message, setMessage] = useState('');
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const handleInit = (data: { broadcast: string }) => {
      if (data.broadcast && data.broadcast.trim()) {
        setMessage(data.broadcast);
        setVisible(true);
      }
    };

    const handleUpdate = (data: { message: string }) => {
      if (data.message && data.message.trim()) {
        setMessage(data.message);
        setVisible(true);
      } else {
        setVisible(false);
      }
    };

    const unsub1 = eventBus.on('state:init', handleInit as any);
    const unsub2 = eventBus.on('broadcast:update', handleUpdate as any);
    return () => {
      unsub1();
      unsub2();
    };
  }, []);

  if (!visible || !message) return null;

  return (
    <div className="broadcast-banner">
      <span className="broadcast-icon">📢</span>
      <span className="broadcast-text">{message}</span>
      <button className="broadcast-close" onClick={() => setVisible(false)}>
        ×
      </button>
    </div>
  );
};

export default BroadcastBanner;
