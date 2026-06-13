import { Pet, CareRecord, PetStatus, StatusChange, ReminderConfig, ShiftSummary } from '../types';
import { eventBus } from './EventBus';

interface InitDataMessage {
  type: 'INIT';
  pets: Pet[];
  recentRecords: CareRecord[];
  lastCareTimeByPet: Record<string, string>;
  lastCareTimeByPetAndAction: Record<string, Record<string, string>>;
  attentionPetIds: string[];
  reminderConfigs: ReminderConfig[];
}

interface PetAddedMessage {
  type: 'PET_ADDED';
  pet: Pet;
}

interface StatusUpdatedMessage {
  type: 'STATUS_UPDATED';
  pet: Pet;
  statusChange: StatusChange;
}

interface CareRecordAddedMessage {
  type: 'CARE_RECORD_ADDED';
  record: CareRecord;
}

interface PetUpdatedMessage {
  type: 'PET_UPDATED';
  pet: Pet;
}

interface CareRecordDeletedMessage {
  type: 'CARE_RECORD_DELETED';
  recordId: string;
}

interface AttentionUpdateMessage {
  type: 'ATTENTION_UPDATE';
  attentionPetIds: string[];
  lastCareTimeByPet: Record<string, string>;
  lastCareTimeByPetAndAction: Record<string, Record<string, string>>;
}

interface ReminderConfigUpdatedMessage {
  type: 'REMINDER_CONFIG_UPDATED';
  reminderConfigs: ReminderConfig[];
}

interface PetDetailMessage {
  type: 'PET_DETAIL';
  pet: Pet;
  careRecords: CareRecord[];
  statusChanges: StatusChange[];
}

interface ShiftSummaryMessage {
  type: 'SHIFT_SUMMARY';
  summary: ShiftSummary;
}

type ServerMessage =
  | InitDataMessage
  | PetAddedMessage
  | StatusUpdatedMessage
  | CareRecordAddedMessage
  | PetUpdatedMessage
  | CareRecordDeletedMessage
  | AttentionUpdateMessage
  | ReminderConfigUpdatedMessage
  | PetDetailMessage
  | ShiftSummaryMessage;

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
            lastCareTimeByPetAndAction: message.lastCareTimeByPetAndAction,
            attentionPetIds: message.attentionPetIds,
            reminderConfigs: message.reminderConfigs,
          });
          break;
        case 'PET_ADDED':
          eventBus.emit('petAdded', message.pet);
          break;
        case 'STATUS_UPDATED':
          eventBus.emit('statusUpdated', { pet: message.pet, statusChange: message.statusChange });
          break;
        case 'CARE_RECORD_ADDED':
          eventBus.emit('careRecordAdded', message.record);
          break;
        case 'PET_UPDATED':
          eventBus.emit('petUpdated', message.pet);
          break;
        case 'CARE_RECORD_DELETED':
          eventBus.emit('careRecordDeleted', message.recordId);
          break;
        case 'ATTENTION_UPDATE':
          eventBus.emit('attentionUpdate', {
            attentionPetIds: message.attentionPetIds,
            lastCareTimeByPet: message.lastCareTimeByPet,
            lastCareTimeByPetAndAction: message.lastCareTimeByPetAndAction,
          });
          break;
        case 'REMINDER_CONFIG_UPDATED':
          eventBus.emit('reminderConfigUpdated', message.reminderConfigs);
          break;
        case 'PET_DETAIL':
          eventBus.emit('petDetail', {
            pet: message.pet,
            careRecords: message.careRecords,
            statusChanges: message.statusChanges,
          });
          break;
        case 'SHIFT_SUMMARY':
          eventBus.emit('shiftSummary', message.summary);
          break;
      }
    } catch (e) {
      console.error('[WebSocket] Parse error:', e);
    }
  }

  addPet(name: string, breed: string, ownerPhoneLast4: string): void {
    this.send({ type: 'ADD_PET', name, breed, ownerPhoneLast4 });
  }

  updateStatus(petId: string, status: PetStatus, staffName: string): void {
    this.send({ type: 'UPDATE_STATUS', petId, status, staffName });
  }

  addCareRecord(petId: string, action: string, note: string, staffName: string): void {
    this.send({ type: 'ADD_CARE_RECORD', petId, action, note, staffName });
  }

  updatePet(petId: string, name: string, breed: string, ownerPhoneLast4: string): void {
    this.send({ type: 'UPDATE_PET', petId, name, breed, ownerPhoneLast4 });
  }

  deleteCareRecord(recordId: string): void {
    this.send({ type: 'DELETE_CARE_RECORD', recordId });
  }

  setReminderConfig(action: string, intervalMinutes: number, enabled: boolean): void {
    this.send({ type: 'SET_REMINDER_CONFIG', action, intervalMinutes, enabled });
  }

  getPetDetail(petId: string): void {
    this.send({ type: 'GET_PET_DETAIL', petId });
  }

  getShiftSummary(): void {
    this.send({ type: 'GET_SHIFT_SUMMARY' });
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
