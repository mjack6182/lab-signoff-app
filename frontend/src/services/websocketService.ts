// websocketService.ts
import { Client, Frame, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

type CheckpointUpdate = {
  groupId: string;
  checkpointNumber: number;
  status: 'PASS' | 'RETURN';
};

type UpdateCallback = (update: CheckpointUpdate) => void;

class WebsocketService {
  private client: Client | null = null;
  private connected = false;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private listeners: Set<UpdateCallback> = new Set();
  private reconnectDelay = 2000; // ms initial, will backoff on repeated failures
  private url = 'http://localhost:8080/ws'; // SockJS endpoint (http), STOMP endpoint on server

  init() {
    if (this.client) return;

    this.client = new Client({
      // when using SockJS: leave brokerURL undefined and provide webSocketFactory
      webSocketFactory: () => new SockJS(this.url),
      debug: (str) => {
        // Optional: route debug logs somewhere else in prod
        // console.debug('[STOMP]', str);
      },
      // reconnect options handled below manually
      reconnectDelay: 0, // we'll implement custom reconnect to control backoff
      onConnect: (frame: Frame) => {
        this.connected = true;
        console.info('[WebsocketService] connected to STOMP', frame);
        // Re-subscribe previously requested topics (if any)
        // For a simple app we can subscribe to a global topic here if desired.
      },
      onStompError: (frame) => {
        console.error('[WebsocketService] STOMP error', frame.headers, frame.body);
      },
      onWebSocketClose: () => {
        this.connected = false;
        console.warn('[WebsocketService] socket closed — scheduling reconnect');
        this.scheduleReconnect();
      },
      onWebSocketError: (e) => {
        console.error('[WebsocketService] websocket error', e);
      },
    });

    // activate triggers the connection
    this.client.activate();
  }

  private scheduleReconnect() {
    const delay = Math.min(this.reconnectDelay, 10000);
    setTimeout(() => {
      if (!this.client || this.connected) return;
      console.info('[WebsocketService] attempting reconnect');
      try {
        this.client!.activate();
      } catch (err) {
        console.error('[WebsocketService] reconnect failed', err);
        // increase backoff
        this.reconnectDelay = Math.min(10000, this.reconnectDelay * 1.5);
        this.scheduleReconnect();
      }
    }, this.reconnectDelay);
    // small backoff increase next time
    this.reconnectDelay = Math.min(10000, this.reconnectDelay * 1.5);
  }

  subscribeToGroup(groupId: string) {
    if (!this.client) this.init();
    if (!this.client) return;
    const topic = `/topic/group-updates`; // single topic; we will filter by groupId in message
    if (this.subscriptions.has(groupId)) return;

    // ensure client is activated
    const sub = this.client.subscribe(topic, (message: IMessage) => {
      try {
        const body = JSON.parse(message.body) as CheckpointUpdate;
        if (body.groupId === groupId) {
          this.listeners.forEach((cb) => cb(body));
        }
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

  disconnect() {
    this.subscriptions.forEach((_, groupId) => this.unsubscribeFromGroup(groupId));
    this.listeners.clear();
    if (this.client) {
      try {
        this.client.deactivate();
      } catch (err) {
        console.warn('[WebsocketService] error on deactivate', err);
      }
      this.client = null;
      this.connected = false;
    }
  }
}

export const websocketService = new WebsocketService();
export type { CheckpointUpdate };