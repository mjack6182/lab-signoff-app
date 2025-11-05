import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { api } from '../config/api';

let client;

export const createWebSocketClient = () => {
  const socketFactory = () => new SockJS(api.ws()); // matches backend endpoint (http://localhost:8080/ws)

  client = new Client({
    webSocketFactory: socketFactory,
    debug: (str) => console.log(str),
    reconnectDelay: 2000, // STOMP reconnection after a disconnect
  });

  client.onConnect = () => {
    console.log('✅ WebSocket connected');

    // ✅ FIXED: Subscribe to correct topic
    client.subscribe('/topic/group-updates', (message) => {
      try {
        const body = JSON.parse(message.body);
        console.log('📡 Received CheckpointUpdate:', body);
      } catch (err) {
        console.error('Failed to parse message:', err);
      }
    });
  };

  client.onStompError = (frame) => {
    console.error('❌ Broker error:', frame.headers['message'], frame.body);
  };

  client.onWebSocketClose = () => {
    console.log('🔴 WebSocket disconnected, will retry...');
    tryConnect(); // manual retry if closed
  };

  // Manual reconnect logic
  const tryConnect = () => {
    if (!client.active) {
      console.log('Attempting WebSocket connection...');
      client.activate();
    }
  };

  tryConnect(); // initial attempt

  return client;
};