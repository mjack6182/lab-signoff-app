package com.example.lab_signoff_backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket handler for processing text messages.
 *
 * This handler processes incoming WebSocket messages from clients
 * and can broadcast updates about lab signoff status changes in real-time.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    /**
     * Handles incoming text messages from WebSocket clients.
     *
     * @param session The WebSocket session
     * @param message The incoming text message
     * @throws Exception if an error occurs during message processing
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Echo back whatever the client sends
        session.sendMessage(new TextMessage("Echo: " + message.getPayload()));
    }
}
