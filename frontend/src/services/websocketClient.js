import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { api } from '../config/api';

let client;

export const createWebSocketClient = () => {
  const socketFactory = () => new SockJS(api.ws()); // matches backend endpoint

  client = new Client({
    webSocketFactory: socketFactory,
    debug: (str) => console.log(str),
    reconnectDelay: 2000, // STOMP reconnection after a disconnect
  });

  client.onConnect = () => {
    console.log('✅ WebSocket connected');

    // Subscribe to your topic
    client.subscribe('/topic/lab-updates', (message) => {
      console.log('Received update:', message.body);
    });
  };

  client.onStompError = (frame) => {
    console.error('❌ Broker error:', frame.headers['message'], frame.body);
  };

  client.onWebSocketClose = () => {
    console.log('🔴 WebSocket disconnected, will retry...');
    tryConnect(); // manual retry if closed
  };

  // Function to attempt connecting
  const tryConnect = () => {
    if (!client.active) {
      console.log('Attempting WebSocket connection...');
      client.activate();
    }
  };

  tryConnect(); // initial attempt

  return client;
};