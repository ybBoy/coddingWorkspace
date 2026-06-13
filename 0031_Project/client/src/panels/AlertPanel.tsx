import { useEffect, useState } from 'react';
import { eventBus, Events } from '../base/EventBus';
import type { AlertItem } from '../base/types';
import { AlertTypeText } from '../base/types';

export function AlertPanel() {
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [expanded, setExpanded] = useState(true);

  useEffect(() => {
    const handler = (data: AlertItem[]) => setAlerts(data);
    eventBus.on(Events.ALERTS_UPDATED, handler);
    return () => eventBus.off(Events.ALERTS_UPDATED, handler);
  }, []);

  if (alerts.length === 0) {
    return null;
  }

  const getAlertClass = (type: string) => {
    switch (type) {
      case 'overdue':
        return 'alert-item alert-overdue';
      case 'maintenance':
        return 'alert-item alert-maintenance';
      case 'dirty':
        return 'alert-item alert-dirty';
      default:
        return 'alert-item';
    }
  };

  const getAlertIcon = (type: string) => {
    switch (type) {
      case 'overdue':
        return '⏰';
      case 'maintenance':
        return '🔧';
      case 'dirty':
        return '🧹';
      default:
        return '⚠️';
    }
  };

  return (
    <div className="alert-panel">
      <div className="alert-header" onClick={() => setExpanded(!expanded)}>
        <span className="alert-title">
          🔔 异常提醒
          <span className="alert-badge">{alerts.length}</span>
        </span>
        <span className="alert-toggle">{expanded ? '▼' : '▶'}</span>
      </div>
      {expanded && (
        <div className="alert-list">
          {alerts.map((alert) => (
            <div key={alert.id} className={getAlertClass(alert.type)}>
              <span className="alert-icon">{getAlertIcon(alert.type)}</span>
              <div className="alert-content">
                <div className="alert-type">{AlertTypeText[alert.type]}</div>
                <div className="alert-message">{alert.message}</div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
