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
  private roomId: string = '';
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
    const params: string[] = [];
    if (this.userName) params.push(`name=${encodeURIComponent(this.userName)}`);
    if (this.roomId) params.push(`room=${encodeURIComponent(this.roomId)}`);
    if (params.length > 0) url += `?${params.join('&')}`;
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
      if (this.roomId) {
        this.listRooms();
      }
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

  setRoomId(roomId: string): void {
    this.roomId = roomId;
  }

  getRoomId(): string {
    return this.roomId;
  }

  listRooms(): void {
    this.sendRaw({ type: 'LIST_ROOMS', payload: null, sender: this.userName });
  }

  createRoom(name: string, passcode?: string): void {
    this.sendRaw({
      type: 'CREATE_ROOM',
      payload: { name, passcode: passcode || '' },
      sender: this.userName
    });
  }

  joinRoom(roomId: string, passcode?: string): void {
    this.roomId = roomId;
    this.sendRaw({
      type: 'JOIN_ROOM',
      payload: { roomId, passcode: passcode || '' },
      sender: this.userName
    });
  }

  leaveRoom(): void {
    const roomId = this.roomId;
    this.sendRaw({
      type: 'LEAVE_ROOM',
      payload: { roomId },
      sender: this.userName
    });
    this.roomId = '';
  }

  setModerator(moderator: boolean, target?: string): void {
    this.sendRaw({
      type: 'SET_MODERATOR',
      payload: { moderator, target },
      sender: this.userName
    });
  }

  updatePresence(paragraphId?: string, typing?: boolean): void {
    this.sendRaw({
      type: 'PRESENCE_UPDATE',
      payload: { paragraphId, typing },
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

  addReply(noteId: string, content: string, parentReplyId?: string): void {
    this.sendRaw({
      type: 'ADD_REPLY',
      payload: { noteId, parentReplyId, content },
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

  toggleLikeReply(replyId: string): void {
    this.sendRaw({
      type: 'TOGGLE_LIKE_REPLY',
      payload: { replyId },
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

  addToQueue(noteId: string): void {
    this.sendRaw({
      type: 'ADD_TO_QUEUE',
      payload: { noteId },
      sender: this.userName
    });
  }

  removeFromQueue(noteId: string): void {
    this.sendRaw({
      type: 'REMOVE_FROM_QUEUE',
      payload: { noteId },
      sender: this.userName
    });
  }

  reorderQueue(order: string[]): void {
    this.sendRaw({
      type: 'REORDER_QUEUE',
      payload: { order },
      sender: this.userName
    });
  }

  importArticle(title: string | undefined, author: string | undefined, text: string): void {
    this.sendRaw({
      type: 'IMPORT_ARTICLE',
      payload: { title, author, text },
      sender: this.userName
    });
  }

  clearNotesByParagraph(paragraphId: string): void {
    this.sendRaw({
      type: 'CLEAR_NOTES_PARAGRAPH',
      payload: { paragraphId },
      sender: this.userName
    });
  }

  exportMarkdown(): void {
    this.sendRaw({ type: 'EXPORT_MARKDOWN', payload: null, sender: this.userName });
  }

  exportJson(): void {
    this.sendRaw({ type: 'EXPORT_JSON', payload: null, sender: this.userName });
  }

  getTimeline(): void {
    this.sendRaw({ type: 'GET_TIMELINE', payload: null, sender: this.userName });
  }

  private sendRaw(msg: WsMessage): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(msg));
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private handleIncoming(msg: WsMessage): void {
    switch (msg.type) {
      case 'INIT':
      case 'STATE_SYNC':
        eventBus.emit('STATE_SYNC', msg.payload as any);
        break;
      case 'ROOM_LIST':
        eventBus.emit('ROOM_LIST', msg.payload as any);
        break;
      case 'ROOM_CREATED':
        eventBus.emit('ROOM_CREATED', msg.payload as any);
        break;
      case 'ROOM_JOINED':
        eventBus.emit('ROOM_JOINED', msg.payload as any);
        break;
      case 'NOTE_ADDED':
        eventBus.emit('NOTE_ADDED', msg.payload as any);
        break;
      case 'REPLY_ADDED':
        eventBus.emit('REPLY_ADDED', msg.payload as any);
        break;
      case 'LIKE_UPDATED':
        eventBus.emit('LIKE_UPDATED', msg.payload as any);
        break;
      case 'REPLY_LIKE_UPDATED':
        eventBus.emit('REPLY_LIKE_UPDATED', msg.payload as any);
        break;
      case 'HIGHLIGHT_UPDATED':
        eventBus.emit('HIGHLIGHT_UPDATED', msg.payload as any);
        break;
      case 'PARAGRAPH_SWITCHED':
        eventBus.emit('PARAGRAPH_SWITCHED', msg.payload as any);
        break;
      case 'PRESENCE_UPDATED':
      case 'ONLINE_COUNT':
        eventBus.emit('PRESENCE_UPDATED', msg.payload as any);
        break;
      case 'MODERATOR_LIST':
        eventBus.emit('MODERATOR_LIST', msg.payload as any);
        break;
      case 'DISCUSSION_QUEUE_UPDATED':
        eventBus.emit('DISCUSSION_QUEUE_UPDATED', msg.payload as any);
        break;
      case 'ARTICLE_UPDATED':
        eventBus.emit('ARTICLE_UPDATED', msg.payload as any);
        break;
      case 'EXPORT_RESULT':
        eventBus.emit('EXPORT_RESULT', msg.payload as any);
        break;
      case 'TIMELINE_DATA':
        eventBus.emit('TIMELINE_DATA', msg.payload as any);
        break;
      case 'MODERATOR_GRANTED':
        eventBus.emit('MODERATOR_GRANTED', msg.payload as any);
        break;
      case 'MODERATOR_DENIED':
        eventBus.emit('MODERATOR_DENIED', msg.payload as any);
        break;
      case 'ROOM_STATE':
        eventBus.emit('ROOM_STATE', msg.payload as any);
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
