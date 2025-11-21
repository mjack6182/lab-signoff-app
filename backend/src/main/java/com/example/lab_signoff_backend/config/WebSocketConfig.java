package com.example.lab_signoff_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
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
 * Allowed origins are configured via the WEBSOCKET_ALLOWED_ORIGINS environment variable (comma-separated).
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // Client connects here (e.g., SockJS + STOMP)
        String[] origins = allowedOrigins.split(",");
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins)
                .withSockJS(); // fallback for older browsers
    }

    /**
     * Configures the broker responsible for routing messages between clients and server.
     *
     * @param registry The message broker registry
     */
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        // Prefix for messages from server → clients
        registry.enableSimpleBroker("/topic");

        // Prefix for client → server destinations
        registry.setApplicationDestinationPrefixes("/app");
    }
}
