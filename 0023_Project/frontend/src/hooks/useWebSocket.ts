import { useEffect, useRef, useCallback } from 'react';
import { eventBus } from '../utils/EventBus';
import { DanmakuMessage, OperationLog } from '../types';

const WS_URL = 'ws://' + window.location.host + '/ws';

export function useWebSocket() {
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);

  const connect = useCallback(() => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) return;
    try {
      const ws = new WebSocket(WS_URL);
      wsRef.current = ws;
      ws.onopen = () => { eventBus.emit('WS_CONNECTED'); };
      ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data);
          handleMessage(msg);
        } catch (e) { console.error('[WS] Parse error:', e); }
      };
      ws.onclose = () => { eventBus.emit('WS_DISCONNECTED'); scheduleReconnect(); };
      ws.onerror = () => {};
    } catch (e) {
      console.error('[WS] Connect error:', e);
      scheduleReconnect();
    }
  }, []);

  const scheduleReconnect = () => {
    if (reconnectTimerRef.current) window.clearTimeout(reconnectTimerRef.current);
    reconnectTimerRef.current = window.setTimeout(() => { connect(); }, 3000);
  };

  const handleMessage = (msg: any) => {
    const { type, data } = msg;
    switch (type) {
      case 'INITIAL_STATE': eventBus.emit('SETTING_UPDATED', data); break;
      case 'SETTING_UPDATED': eventBus.emit('SETTING_UPDATED', data); break;
      case 'NEW_MESSAGE': eventBus.emit('NEW_MESSAGE', data as DanmakuMessage); break;
      case 'NEW_PENDING': eventBus.emit('NEW_PENDING', data as DanmakuMessage); break;
      case 'PENDING_UPDATED': eventBus.emit('PENDING_UPDATED', data as DanmakuMessage); break;
      case 'PENDING_LIST': eventBus.emit('PENDING_LIST', data as DanmakuMessage[]); break;
      case 'MESSAGE_QUEUED': eventBus.emit('MESSAGE_QUEUED', data as DanmakuMessage); break;
      case 'CLEAR_SCREEN': eventBus.emit('CLEAR_SCREEN'); break;
      case 'SENDING_DISABLED': eventBus.emit('SENDING_DISABLED'); break;
      case 'SEND_REJECTED': eventBus.emit('SEND_REJECTED', data); break;
      case 'HISTORY_MESSAGES': eventBus.emit('HISTORY_MESSAGES', data as DanmakuMessage[]); break;
      case 'AUTH_FAILED': eventBus.emit('AUTH_FAILED'); break;
      case 'AUTH_SUCCESS': eventBus.emit('AUTH_SUCCESS', data); break;
      case 'PIN_UPDATED': eventBus.emit('PIN_UPDATED', data as DanmakuMessage); break;
      case 'ONLINE_COUNT': eventBus.emit('ONLINE_COUNT', data); break;
      case 'PLAYBACK_STATE': eventBus.emit('PLAYBACK_STATE', data); break;
      case 'OPERATION_LOGS': eventBus.emit('OPERATION_LOGS', data as OperationLog[]); break;
      case 'EXPORT_DONE': eventBus.emit('EXPORT_DONE', data); break;
      case 'BACKUP_DONE': eventBus.emit('BACKUP_DONE'); break;
      case 'APPROVED_LIST': eventBus.emit('APPROVED_LIST', data as DanmakuMessage[]); break;
    }
  };

  const send = useCallback((type: string, data?: any) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ type, data }));
    }
  }, []);

  useEffect(() => {
    connect();
    const events = [
      ['SEND_MESSAGE', (d: any) => send('SEND_MESSAGE', d)],
      ['APPROVE_MESSAGE', (d: any) => send('APPROVE_MESSAGE', d)],
      ['REJECT_MESSAGE', (d: any) => send('REJECT_MESSAGE', d)],
      ['CLEAR_SCREEN', () => send('CLEAR_SCREEN')],
      ['TOGGLE_SENDING', (d: any) => send('TOGGLE_SENDING', d)],
      ['SET_ROLE', (d: any) => send('SET_ROLE', d)],
      ['GET_PENDING', () => send('GET_PENDING')],
      ['GET_HISTORY', () => send('GET_HISTORY')],
      ['TOGGLE_PLAYBACK', (d: any) => send('TOGGLE_PLAYBACK', d)],
      ['TOGGLE_PIN', (d: any) => send('TOGGLE_PIN', d)],
      ['APPROVE_NORMAL_ONLY', () => send('APPROVE_NORMAL_ONLY')],
      ['UPDATE_SETTINGS', (d: any) => send('UPDATE_SETTINGS', d)],
      ['GET_LOGS', () => send('GET_LOGS')],
      ['EXPORT_DATA', () => send('EXPORT_DATA')],
      ['ROTATE_BACKUP', () => send('ROTATE_BACKUP')],
      ['VALIDATE_TOKEN', (d: any) => send('VALIDATE_TOKEN', d)],
      ['APPROVE_AND_PIN', (d: any) => send('APPROVE_AND_PIN', d)],
      ['GET_APPROVED', () => send('GET_APPROVED')],
    ] as const;
    const unsubs = events.map(([ev, fn]) => eventBus.on(ev as any, fn));
    return () => {
      unsubs.forEach(u => u());
      if (reconnectTimerRef.current) window.clearTimeout(reconnectTimerRef.current);
      if (wsRef.current) wsRef.current.close();
    };
  }, [connect, send]);

  return { send };
}
