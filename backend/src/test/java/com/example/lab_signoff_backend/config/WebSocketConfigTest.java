package com.example.lab_signoff_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebSocketConfigTest {

    @Test
    void registerStompEndpoints_setsAllowedOrigins() {
        WebSocketConfig config = new WebSocketConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://a.com,http://b.com");
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class, RETURNS_DEEP_STUBS);

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/ws");
    }

    @Test
    void configureMessageBroker_setsPrefixes() {
        WebSocketConfig config = new WebSocketConfig();
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic");
        verify(registry).setApplicationDestinationPrefixes("/app");
    }
}
