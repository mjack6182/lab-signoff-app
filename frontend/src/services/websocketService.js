import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let client;
let listeners = [];
let statusListeners = [];

export const websocketService = {
  init: () => {
    if (client) return; // already initialized

    client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('✅ WebSocket connected');
        statusListeners.forEach(fn => fn('CONNECTED'));
      },
      onStompError: (frame) => console.error('STOMP Error:', frame),
      onDisconnect: () => statusListeners.forEach(fn => fn('DISCONNECTED')),
    });

    client.activate();
  },

  subscribeToGroup: (groupId) => {
    if (!client) return;
    return client.subscribe('/topic/group-updates', (message) => {
      const data = JSON.parse(message.body);
      listeners.forEach(fn => fn(data));
    });
  },

  unsubscribeFromGroup: (groupId) => {
    if (!client || !client.subscriptions) return;
    Object.values(client.subscriptions).forEach(sub => sub.unsubscribe());
  },

  addListener: (fn) => {
    listeners.push(fn);
  },

  removeListener: (fn) => {
    listeners = listeners.filter(f => f !== fn);
  },

  addStatusListener: (fn) => {
    statusListeners.push(fn);
  },

  removeStatusListener: (fn) => {
    statusListeners = statusListeners.filter(f => f !== fn);
  },
};