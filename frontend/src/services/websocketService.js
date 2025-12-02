// src/services/websocketService.js
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws'

let client = null

// Use Sets to automatically prevent duplicates
let listeners = new Set()
let statusListeners = new Set()
let activeSubscriptions = new Map()

export const websocketService = {
  init() {
    if (client) return

    client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      debug: (msg) => console.log(msg),

      onConnect: () => {
        console.log('WS CONNECTED')
        statusListeners.forEach(fn => fn('CONNECTED'))
      },

      onStompError: (frame) => console.error('STOMP Error:', frame),
      onDisconnect: () => {
        console.log('WS DISCONNECTED')
        statusListeners.forEach(fn => fn('DISCONNECTED'))
      }
    })

    client.activate()
  },

  subscribe(topic) {
    if (!client || !client.active) {
      console.warn('Cannot subscribe. Client not active')
      return
    }

    if (activeSubscriptions.has(topic)) return

    const sub = client.subscribe(topic, (message) => {
      let data
      try {
        data = JSON.parse(message.body)
      } catch (e) {
        console.error('WS parse error:', e)
        return
      }
      const destination = message.headers?.destination
      listeners.forEach(fn => fn(data, destination))
    })

    activeSubscriptions.set(topic, sub)
    console.log(`Subscribed to ${topic}`)
  },

  unsubscribe(topic) {
    const sub = activeSubscriptions.get(topic)
    if (sub) {
      sub.unsubscribe()
      activeSubscriptions.delete(topic)
      console.log(`Unsubscribed from ${topic}`)
    }
  },

  addListener(fn) {
    listeners.add(fn)
  },

  removeListener(fn) {
    listeners.delete(fn)
  },

  addStatusListener(fn) {
    statusListeners.add(fn)
  },

  removeStatusListener(fn) {
    statusListeners.delete(fn)
  },

  disconnect() {
    if (client) {
      activeSubscriptions.forEach(sub => sub.unsubscribe())
      activeSubscriptions.clear()
      client.deactivate()
      client = null
      listeners.clear()
      statusListeners.clear()
    }
  }
}
