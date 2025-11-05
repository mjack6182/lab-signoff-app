import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  CheckpointUpdate,
  GroupStatusUpdate,
  HelpQueueUpdate,
  WebSocketTopics,
} from '../types/websocket';

type ConnectionStatus = 'CONNECTED' | 'RECONNECTING' | 'DISCONNECTED';

type MessageCallback<T> = (message: T) => void;
type StatusCallback = (status: ConnectionStatus) => void;

/**
 * Enhanced WebSocket service with support for new database schema
 * Supports lab-specific and group-specific subscriptions
 */
class WebSocketServiceEnhanced {
  private client: Client | null = null;
  private connected = false;
  private subscriptions: Map<string, StompSubscription> = new Map();

  // Listeners by event type
  private checkpointListeners: Set<MessageCallback<CheckpointUpdate>> = new Set();
  private groupStatusListeners: Set<MessageCallback<GroupStatusUpdate>> = new Set();
  private helpQueueListeners: Set<MessageCallback<HelpQueueUpdate>> = new Set();
  private statusListeners: Set<StatusCallback> = new Set();

  private reconnectDelay = 2000;
  private url = 'http://localhost:8080/ws';

  /**
   * Initialize WebSocket connection
   */
  init() {
    if (this.client) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(this.url),
      debug: (str) => console.log('[STOMP]', str),
      reconnectDelay: 0,
      onConnect: () => {
        this.connected = true;
        this.notifyStatus('CONNECTED');
        console.info('[WebSocketService] Connected');
        // Re-subscribe to all topics
        this.resubscribeAll();
      },
      onWebSocketClose: () => {
        this.connected = false;
        console.warn('[WebSocketService] Connection closed - reconnecting...');
        this.notifyStatus('RECONNECTING');
        this.scheduleReconnect();
      },
      onWebSocketError: (e) => {
        console.error('[WebSocketService] WebSocket error', e);
        this.notifyStatus('DISCONNECTED');
      },
    });

    this.client.activate();
  }

  private scheduleReconnect() {
    const delay = Math.min(this.reconnectDelay, 10000);
    setTimeout(() => {
      if (!this.client || this.connected) return;
      console.info('[WebSocketService] Attempting reconnect');
      try {
        this.client!.activate();
      } catch {
        this.reconnectDelay = Math.min(10000, this.reconnectDelay * 1.5);
        this.scheduleReconnect();
      }
    }, delay);
    this.reconnectDelay = Math.min(10000, this.reconnectDelay * 1.5);
  }

  private resubscribeAll() {
    // Resubscribe will happen automatically when components call subscribe methods
  }

  /**
   * Subscribe to checkpoint updates for a specific lab
   */
  subscribeToLabCheckpoints(labId: string) {
    if (!this.client) this.init();
    const topic = WebSocketTopics.labCheckpoints(labId);

    if (this.subscriptions.has(topic)) return;

    const sub = this.client!.subscribe(topic, (message: IMessage) => {
      try {
        const update: CheckpointUpdate = JSON.parse(message.body);
        this.checkpointListeners.forEach((cb) => cb(update));
      } catch (err) {
        console.error('[WebSocketService] Failed to parse checkpoint update', err);
      }
    });

    this.subscriptions.set(topic, sub);
    console.info(`[WebSocketService] Subscribed to ${topic}`);
  }

  /**
   * Subscribe to group status updates for a specific lab
   */
  subscribeToLabGroups(labId: string) {
    if (!this.client) this.init();
    const topic = WebSocketTopics.labGroups(labId);

    if (this.subscriptions.has(topic)) return;

    const sub = this.client!.subscribe(topic, (message: IMessage) => {
      try {
        const update: GroupStatusUpdate = JSON.parse(message.body);
        this.groupStatusListeners.forEach((cb) => cb(update));
      } catch (err) {
        console.error('[WebSocketService] Failed to parse group status update', err);
      }
    });

    this.subscriptions.set(topic, sub);
    console.info(`[WebSocketService] Subscribed to ${topic}`);
  }

  /**
   * Subscribe to help queue updates for a specific lab
   */
  subscribeToLabHelpQueue(labId: string) {
    if (!this.client) this.init();
    const topic = WebSocketTopics.labHelpQueue(labId);

    if (this.subscriptions.has(topic)) return;

    const sub = this.client!.subscribe(topic, (message: IMessage) => {
      try {
        const update: HelpQueueUpdate = JSON.parse(message.body);
        this.helpQueueListeners.forEach((cb) => cb(update));
      } catch (err) {
        console.error('[WebSocketService] Failed to parse help queue update', err);
      }
    });

    this.subscriptions.set(topic, sub);
    console.info(`[WebSocketService] Subscribed to ${topic}`);
  }

  /**
   * Subscribe to checkpoint updates for a specific group
   */
  subscribeToGroupCheckpoints(groupId: string) {
    if (!this.client) this.init();
    const topic = WebSocketTopics.groupCheckpoints(groupId);

    if (this.subscriptions.has(topic)) return;

    const sub = this.client!.subscribe(topic, (message: IMessage) => {
      try {
        const update: CheckpointUpdate = JSON.parse(message.body);
        this.checkpointListeners.forEach((cb) => cb(update));
      } catch (err) {
        console.error('[WebSocketService] Failed to parse checkpoint update', err);
      }
    });

    this.subscriptions.set(topic, sub);
    console.info(`[WebSocketService] Subscribed to ${topic}`);
  }

  /**
   * Subscribe to all updates for a specific lab (convenience method)
   */
  subscribeToLab(labId: string) {
    this.subscribeToLabCheckpoints(labId);
    this.subscribeToLabGroups(labId);
    this.subscribeToLabHelpQueue(labId);
  }

  /**
   * Unsubscribe from a specific topic
   */
  unsubscribe(topic: string) {
    const sub = this.subscriptions.get(topic);
    if (sub) {
      sub.unsubscribe();
      this.subscriptions.delete(topic);
      console.info(`[WebSocketService] Unsubscribed from ${topic}`);
    }
  }

  /**
   * Unsubscribe from all lab topics
   */
  unsubscribeFromLab(labId: string) {
    this.unsubscribe(WebSocketTopics.labCheckpoints(labId));
    this.unsubscribe(WebSocketTopics.labGroups(labId));
    this.unsubscribe(WebSocketTopics.labHelpQueue(labId));
  }

  // Listener management
  onCheckpointUpdate(cb: MessageCallback<CheckpointUpdate>) {
    this.checkpointListeners.add(cb);
  }

  offCheckpointUpdate(cb: MessageCallback<CheckpointUpdate>) {
    this.checkpointListeners.delete(cb);
  }

  onGroupStatusUpdate(cb: MessageCallback<GroupStatusUpdate>) {
    this.groupStatusListeners.add(cb);
  }

  offGroupStatusUpdate(cb: MessageCallback<GroupStatusUpdate>) {
    this.groupStatusListeners.delete(cb);
  }

  onHelpQueueUpdate(cb: MessageCallback<HelpQueueUpdate>) {
    this.helpQueueListeners.add(cb);
  }

  offHelpQueueUpdate(cb: MessageCallback<HelpQueueUpdate>) {
    this.helpQueueListeners.delete(cb);
  }

  onStatusChange(cb: StatusCallback) {
    this.statusListeners.add(cb);
  }

  offStatusChange(cb: StatusCallback) {
    this.statusListeners.delete(cb);
  }

  private notifyStatus(status: ConnectionStatus) {
    this.statusListeners.forEach((cb) => cb(status));
  }

  /**
   * Disconnect and cleanup all subscriptions
   */
  disconnect() {
    this.subscriptions.forEach((_, topic) => this.unsubscribe(topic));
    this.checkpointListeners.clear();
    this.groupStatusListeners.clear();
    this.helpQueueListeners.clear();
    this.statusListeners.clear();

    if (this.client) {
      try {
        this.client.deactivate();
      } catch {}
      this.client = null;
      this.connected = false;
    }
    console.info('[WebSocketService] Disconnected');
  }

  /**
   * Get connection status
   */
  isConnected(): boolean {
    return this.connected;
  }
}

// Export singleton instance
export const websocketService = new WebSocketServiceEnhanced();
