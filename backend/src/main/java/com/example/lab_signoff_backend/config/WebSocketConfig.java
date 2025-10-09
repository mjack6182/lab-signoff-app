package com.example.lab_signoff_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Client will connect here (React, etc.)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")   // allow frontend
                .withSockJS();                   // fallback for older browsers
    }

    @Override
<<<<<<< Updated upstream
<<<<<<< Updated upstream
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myWebSocketHandler, "/ws")
                // Restrict allowed origins to trusted domains only
                .setAllowedOrigins("http://localhost:3000", "https://yourdomain.com");
=======
=======
>>>>>>> Stashed changes
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for messages from server → clients
        registry.enableSimpleBroker("/topic");

        // Prefix for client → server destinations
        registry.setApplicationDestinationPrefixes("/app");
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
    }
}
