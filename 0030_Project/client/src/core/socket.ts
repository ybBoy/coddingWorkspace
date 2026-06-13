import { Pet, CareRecord, PetStatus } from '../types';
import { eventBus } from './EventBus';

interface InitDataMessage {
  type: 'INIT';
  pets: Pet[];
  recentRecords: CareRecord[];
  lastCareTimeByPet: Record<string, string>;
}

interface PetAddedMessage {
  type: 'PET_ADDED';
  pet: Pet;
}

interface StatusUpdatedMessage {
  type: 'STATUS_UPDATED';
  pet: Pet;
}

interface CareRecordAddedMessage {
  type: 'CARE_RECORD_ADDED';
  record: CareRecord;
}

type ServerMessage =
  | InitDataMessage
  | PetAddedMessage
  | StatusUpdatedMessage
  | CareRecordAddedMessage;

class PetSocket {
  private ws: WebSocket | null = null;
  private url: string;
  private reconnectTimer: number | null = null;
  private reconnectDelay: number = 3000;

  constructor() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    this.url = `${protocol}//${host}/ws`;
  }

  connect(): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      return;
    }

    try {
      this.ws = new WebSocket(this.url);

      this.ws.onopen = () => {
        console.log('[WebSocket] Connected');
        eventBus.emit('socketConnected');
        this.clearReconnectTimer();
      };

      this.ws.onclose = () => {
        console.log('[WebSocket] Disconnected');
        eventBus.emit('socketDisconnected');
        this.scheduleReconnect();
      };

      this.ws.onerror = (error) => {
        console.error('[WebSocket] Error:', error);
      };

      this.ws.onmessage = (event) => {
        this.handleMessage(event.data);
      };
    } catch (e) {
      console.error('[WebSocket] Connection failed:', e);
      this.scheduleReconnect();
    }
  }

  disconnect(): void {
    this.clearReconnectTimer();
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  private handleMessage(data: string): void {
    try {
      const message: ServerMessage = JSON.parse(data);

      switch (message.type) {
        case 'INIT':
          eventBus.emit('initData', {
            pets: message.pets,
            recentRecords: message.recentRecords,
            lastCareTimeByPet: message.lastCareTimeByPet,
          });
          break;
        case 'PET_ADDED':
          eventBus.emit('petAdded', message.pet);
          break;
        case 'STATUS_UPDATED':
          eventBus.emit('statusUpdated', message.pet);
          break;
        case 'CARE_RECORD_ADDED':
          eventBus.emit('careRecordAdded', message.record);
          break;
      }
    } catch (e) {
      console.error('[WebSocket] Parse error:', e);
    }
  }

  addPet(name: string, breed: string, ownerPhoneLast4: string): void {
    this.send({
      type: 'ADD_PET',
      name,
      breed,
      ownerPhoneLast4,
    });
  }

  updateStatus(petId: string, status: PetStatus): void {
    this.send({
      type: 'UPDATE_STATUS',
      petId,
      status,
    });
  }

  addCareRecord(petId: string, action: string, note?: string): void {
    this.send({
      type: 'ADD_CARE_RECORD',
      petId,
      action,
      note: note || '',
    });
  }

  private send(data: any): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data));
    } else {
      console.warn('[WebSocket] Not connected, message dropped');
    }
  }

  private scheduleReconnect(): void {
    this.clearReconnectTimer();
    this.reconnectTimer = window.setTimeout(() => {
      console.log('[WebSocket] Reconnecting...');
      this.connect();
    }, this.reconnectDelay);
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }
}

export const petSocket = new PetSocket();
export default petSocket;
