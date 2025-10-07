package com.example.lab_signoff_backend.config;


import com.example.lab_signoff_backend.websocket.MyWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

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
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MyWebSocketHandler myWebSocketHandler;

    /**
     * Constructor for WebSocketConfig.
     *
     * @param myWebSocketHandler The WebSocket handler for processing messages
     */
    public WebSocketConfig(MyWebSocketHandler myWebSocketHandler) {
        this.myWebSocketHandler = myWebSocketHandler;
    }

    /**
     * Registers WebSocket handlers and configures allowed origins.
     *
     * @param registry The WebSocket handler registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myWebSocketHandler, "/ws")
                // Restrict allowed origins to trusted domains only
                .setAllowedOrigins("http://localhost:3000", "https://yourdomain.com");
    }
}
