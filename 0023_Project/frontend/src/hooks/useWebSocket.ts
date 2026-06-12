import { useEffect, useRef, useCallback } from 'react';
import { eventBus } from '../utils/EventBus';
import { DanmakuMessage } from '../types';

const WS_URL = `ws://${window.location.host}/ws`;

export function useWebSocket() {
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);
  const connectedRef = useRef(false);

  const connect = useCallback(() => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      return;
    }

    try {
      const ws = new WebSocket(WS_URL);
      wsRef.current = ws;

      ws.onopen = () => {
        connectedRef.current = true;
        eventBus.emit('WS_CONNECTED');
      };

      ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data);
          handleMessage(msg);
        } catch (e) {
          console.error('[WS] Parse error:', e);
        }
      };

      ws.onclose = () => {
        connectedRef.current = false;
        eventBus.emit('WS_DISCONNECTED');
        scheduleReconnect();
      };

      ws.onerror = () => {
        // error handled by onclose
      };
    } catch (e) {
      console.error('[WS] Connect error:', e);
      scheduleReconnect();
    }
  }, []);

  const scheduleReconnect = () => {
    if (reconnectTimerRef.current) {
      window.clearTimeout(reconnectTimerRef.current);
    }
    reconnectTimerRef.current = window.setTimeout(() => {
      connect();
    }, 3000);
  };

  const handleMessage = (msg: any) => {
    const { type, data } = msg;

    switch (type) {
      case 'INITIAL_STATE':
        eventBus.emit('SETTING_UPDATED', data);
        break;
      case 'NEW_MESSAGE':
        eventBus.emit('NEW_MESSAGE', data as DanmakuMessage);
        break;
      case 'NEW_PENDING':
        eventBus.emit('NEW_PENDING', data as DanmakuMessage);
        break;
      case 'PENDING_UPDATED':
        eventBus.emit('PENDING_UPDATED', data as DanmakuMessage);
        break;
      case 'PENDING_LIST':
        eventBus.emit('PENDING_LIST', data as DanmakuMessage[]);
        break;
      case 'MESSAGE_QUEUED':
        eventBus.emit('MESSAGE_QUEUED', data as DanmakuMessage);
        break;
      case 'CLEAR_SCREEN':
        eventBus.emit('CLEAR_SCREEN');
        break;
      case 'SETTING_UPDATED':
        eventBus.emit('SETTING_UPDATED', data);
        break;
      case 'SENDING_DISABLED':
        eventBus.emit('SENDING_DISABLED');
        break;
      case 'HISTORY_MESSAGES':
        eventBus.emit('HISTORY_MESSAGES', data as DanmakuMessage[]);
        break;
      case 'AUTH_FAILED':
        eventBus.emit('AUTH_FAILED');
        break;
    }
  };

  const send = useCallback((type: string, data?: any) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ type, data }));
    } else {
      console.warn('[WS] Not connected, message dropped:', type);
    }
  }, []);

  useEffect(() => {
    connect();

    const unsub1 = eventBus.on('SEND_MESSAGE', (data) => {
      send('SEND_MESSAGE', data);
    });
    const unsub2 = eventBus.on('APPROVE_MESSAGE', (data) => {
      send('APPROVE_MESSAGE', data);
    });
    const unsub3 = eventBus.on('REJECT_MESSAGE', (data) => {
      send('REJECT_MESSAGE', data);
    });
    const unsub4 = eventBus.on('CLEAR_SCREEN', () => {
      send('CLEAR_SCREEN');
    });
    const unsub5 = eventBus.on('TOGGLE_SENDING', (data) => {
      send('TOGGLE_SENDING', data);
    });
    const unsub6 = eventBus.on('SET_ROLE', (data) => {
      send('SET_ROLE', data);
    });
    const unsub7 = eventBus.on('GET_PENDING', () => {
      send('GET_PENDING');
    });
    const unsub8 = eventBus.on('GET_HISTORY', () => {
      send('GET_HISTORY');
    });

    return () => {
      unsub1();
      unsub2();
      unsub3();
      unsub4();
      unsub5();
      unsub6();
      unsub7();
      unsub8();
      if (reconnectTimerRef.current) {
        window.clearTimeout(reconnectTimerRef.current);
      }
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [connect, send]);

  return { send, connectedRef };
}
