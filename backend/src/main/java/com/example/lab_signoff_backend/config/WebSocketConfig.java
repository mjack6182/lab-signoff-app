package com.example.lab_signoff_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time communication.
 *
 * This class configures WebSocket endpoints and handlers for bidirectional
 * communication between the server and client, enabling real-time updates
 * for lab signoff status changes.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

<<<<<<< HEAD
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Client will connect here (React, etc.)
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000")   // allow only trusted frontend
                .withSockJS();                   // fallback for older browsers
=======
    private final MyWebSocketHandler myWebSocketHandler;

    /**
     * Constructor for WebSocketConfig.
     *
     * @param myWebSocketHandler The WebSocket handler for processing messages
     */
    public WebSocketConfig(MyWebSocketHandler myWebSocketHandler) {
        this.myWebSocketHandler = myWebSocketHandler;
>>>>>>> dev
    }

    /**
     * Registers WebSocket handlers and configures allowed origins.
     *
     * @param registry The WebSocket handler registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for messages from server → clients
        registry.enableSimpleBroker("/topic");

        // Prefix for client → server destinations
        registry.setApplicationDestinationPrefixes("/app");
    }
}
