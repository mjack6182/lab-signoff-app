import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

type CheckpointUpdate = {
  groupId: string;
  checkpointNumber: number;
  status: 'PASS' | 'RETURN';
};

type UpdateCallback = (update: CheckpointUpdate) => void;
type ConnectionStatus = 'CONNECTED' | 'RECONNECTING' | 'DISCONNECTED';
type StatusCallback = (status: ConnectionStatus) => void;

class WebsocketService {
  private client: Client | null = null;
  private connected = false;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private listeners: Set<UpdateCallback> = new Set();
  private statusListeners: Set<StatusCallback> = new Set();
  private reconnectDelay = 2000;
  private url = 'http://localhost:8080/ws';

  init() {
    if (this.client) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(this.url),
      debug: (str) => console.log('[STOMP]', str),
      reconnectDelay: 0,
      onConnect: () => {
        this.connected = true;
        this.notifyStatus('CONNECTED');
        console.info('[WebsocketService] connected');
        // Re-subscribe existing groups
        Array.from(this.subscriptions.keys()).forEach((groupId) => this.subscribeToGroup(groupId));
      },
      onWebSocketClose: () => {
        this.connected = false;
        console.warn('[WebsocketService] socket closed — reconnecting...');
        this.notifyStatus('RECONNECTING');
        this.scheduleReconnect();
      },
      onWebSocketError: (e) => {
        console.error('[WebsocketService] websocket error', e);
        this.notifyStatus('DISCONNECTED');
      },
    });

    this.client.activate();
  }

  private scheduleReconnect() {
    const delay = Math.min(this.reconnectDelay, 10000);
    setTimeout(() => {
      if (!this.client || this.connected) return;
      console.info('[WebsocketService] attempting reconnect');
      try {
        this.client!.activate();
      } catch {
        this.reconnectDelay = Math.min(10000, this.reconnectDelay * 1.5);
        this.scheduleReconnect();
      }
    }, this.reconnectDelay);
    this.reconnectDelay = Math.min(10000, this.reconnectDelay * 1.5);
  }

  subscribeToGroup(groupId: string) {
    if (!this.client) this.init();
    if (!this.client) return;
    if (this.subscriptions.has(groupId)) return;

    const sub = this.client!.subscribe('/topic/group-updates', (message: IMessage) => {
      try {
        const body = JSON.parse(message.body) as CheckpointUpdate;
        // Forward to all listeners, filter by groupId in component
        this.listeners.forEach((cb) => cb(body));
      } catch (err) {
        console.error('[WebsocketService] failed to parse message', err);
      }
    });

    this.subscriptions.set(groupId, sub);
  }

  unsubscribeFromGroup(groupId: string) {
    const sub = this.subscriptions.get(groupId);
    if (sub) {
      sub.unsubscribe();
      this.subscriptions.delete(groupId);
    }
  }

  addListener(cb: UpdateCallback) {
    this.listeners.add(cb);
  }

  removeListener(cb: UpdateCallback) {
    this.listeners.delete(cb);
  }

  addStatusListener(cb: StatusCallback) {
    this.statusListeners.add(cb);
  }

  removeStatusListener(cb: StatusCallback) {
    this.statusListeners.delete(cb);
  }

  private notifyStatus(status: ConnectionStatus) {
    this.statusListeners.forEach((cb) => cb(status));
  }

  disconnect() {
    this.subscriptions.forEach((_, groupId) => this.unsubscribeFromGroup(groupId));
    this.listeners.clear();
    this.statusListeners.clear();
    if (this.client) {
      try {
        this.client.deactivate();
      } catch {}
      this.client = null;
      this.connected = false;
    }
  }
}

export const websocketService = new WebsocketService();
export type { CheckpointUpdate, UpdateCallback, ConnectionStatus };