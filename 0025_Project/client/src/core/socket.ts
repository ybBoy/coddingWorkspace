import eventBus from './EventBus';
import type { SocketStatus, WsMessage, NoteType } from './types';

const DEFAULT_URL = `ws://${window.location.hostname}:8080`;
const RECONNECT_DELAYS = [1000, 2000, 4000, 8000];

class SocketClient {
  private ws: WebSocket | null = null;
  private url: string;
  private status: SocketStatus = 'closed';
  private reconnectAttempt = 0;
  private reconnectTimer: number | null = null;
  private heartbeatTimer: number | null = null;
  private userName: string = '';
  private forceClose = false;

  constructor(url?: string) {
    this.url = url || DEFAULT_URL;
  }

  private setStatus(s: SocketStatus) {
    if (this.status !== s) {
      this.status = s;
      eventBus.emit('SOCKET_STATUS', s);
    }
  }

  private buildConnectUrl(): string {
    let url = this.url;
    if (this.userName) {
      const sep = url.includes('?') ? '&' : '?';
      url += `${sep}name=${encodeURIComponent(this.userName)}`;
    }
    return url;
  }

  connect(): void {
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      return;
    }
    this.forceClose = false;
    this.setStatus('connecting');

    try {
      this.ws = new WebSocket(this.buildConnectUrl());
    } catch (e) {
      this.setStatus('error');
      this.scheduleReconnect();
      return;
    }

    this.ws.onopen = () => {
      this.setStatus('open');
      this.reconnectAttempt = 0;
      this.startHeartbeat();
      if (this.userName) {
        this.sendRaw({ type: 'SET_NAME', payload: this.userName, sender: this.userName });
      }
      this.sendRaw({ type: 'REQUEST_STATE', payload: null, sender: this.userName });
      console.log('[Socket] connected');
    };

    this.ws.onclose = () => {
      this.stopHeartbeat();
      this.setStatus('closed');
      if (!this.forceClose) this.scheduleReconnect();
    };

    this.ws.onerror = () => {
      this.setStatus('error');
    };

    this.ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data) as WsMessage;
        this.handleIncoming(msg);
      } catch (e) {
        console.error('[Socket] parse error', e);
      }
    };
  }

  disconnect(): void {
    this.forceClose = true;
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.stopHeartbeat();
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  getStatus(): SocketStatus {
    return this.status;
  }

  setUserName(name: string): void {
    const prev = this.userName;
    this.userName = name;
    eventBus.emit('USER_NAME_CHANGED', name);
    if (name && name !== prev && this.status === 'open') {
      this.sendRaw({ type: 'SET_NAME', payload: name, sender: name });
    }
  }

  getUserName(): string {
    return this.userName;
  }

  setModerator(moderator: boolean, token?: string): void {
    this.sendRaw({
      type: 'SET_MODERATOR',
      payload: { moderator, token },
      sender: this.userName
    });
  }

  addNote(paragraphId: string, content: string, type: NoteType): void {
    this.sendRaw({
      type: 'ADD_NOTE',
      payload: { paragraphId, content, type },
      sender: this.userName
    });
  }

  toggleLike(noteId: string): void {
    this.sendRaw({
      type: 'TOGGLE_LIKE',
      payload: { noteId },
      sender: this.userName
    });
  }

  toggleHighlight(noteId: string): void {
    this.sendRaw({
      type: 'TOGGLE_HIGHLIGHT',
      payload: { noteId },
      sender: this.userName
    });
  }

  switchParagraph(paragraphId: string): void {
    this.sendRaw({
      type: 'SWITCH_PARAGRAPH',
      payload: { paragraphId },
      sender: this.userName
    });
  }

  moveNext(): void {
    this.sendRaw({ type: 'MOVE_NEXT', payload: null, sender: this.userName });
  }

  movePrev(): void {
    this.sendRaw({ type: 'MOVE_PREV', payload: null, sender: this.userName });
  }

  private sendRaw(msg: WsMessage): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(msg));
    }
  }

  private handleIncoming(msg: WsMessage): void {
    switch (msg.type) {
      case 'INIT':
      case 'STATE_SYNC':
        eventBus.emit('STATE_SYNC', msg.payload as any);
        break;
      case 'NOTE_ADDED':
        eventBus.emit('NOTE_ADDED', msg.payload as any);
        break;
      case 'LIKE_UPDATED':
        eventBus.emit('LIKE_UPDATED', msg.payload as any);
        break;
      case 'HIGHLIGHT_UPDATED':
        eventBus.emit('HIGHLIGHT_UPDATED', msg.payload as any);
        break;
      case 'PARAGRAPH_SWITCHED':
        eventBus.emit('PARAGRAPH_SWITCHED', msg.payload as any);
        break;
      case 'ONLINE_COUNT':
        eventBus.emit('ONLINE_COUNT', msg.payload as any);
        break;
      case 'MODERATOR_LIST':
        eventBus.emit('MODERATOR_LIST', msg.payload as any);
        break;
      case 'MODERATOR_GRANTED':
        eventBus.emit('MODERATOR_GRANTED', msg.payload as any);
        break;
      case 'MODERATOR_DENIED':
        eventBus.emit('MODERATOR_DENIED', msg.payload as any);
        break;
      case 'ERROR':
        eventBus.emit('ERROR', msg.payload as any);
        break;
      case 'HEARTBEAT_ACK':
        break;
    }
  }

  private scheduleReconnect(): void {
    if (this.forceClose) return;
    if (this.reconnectTimer) return;
    const delay = RECONNECT_DELAYS[Math.min(this.reconnectAttempt, RECONNECT_DELAYS.length - 1)];
    this.reconnectAttempt += 1;
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, delay);
  }

  private startHeartbeat(): void {
    this.stopHeartbeat();
    this.heartbeatTimer = window.setInterval(() => {
      if (this.status === 'open') {
        this.sendRaw({ type: 'HEARTBEAT', payload: null, sender: this.userName });
      }
    }, 30000);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      window.clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }
}

export const socket = new SocketClient();
export default socket;
