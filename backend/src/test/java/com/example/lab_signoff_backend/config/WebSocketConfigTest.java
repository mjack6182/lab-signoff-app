package com.example.lab_signoff_backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.*;
import static org.mockito.AdditionalMatchers.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private StompEndpointRegistry stompEndpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    private WebSocketConfig config;

    @BeforeEach
    void setUp() {
        config = new WebSocketConfig();

        // this fixes the NPE issue
        lenient().when(stompEndpointRegistry.addEndpoint("/ws"))
                .thenReturn(endpointRegistration);

        lenient().when(
                endpointRegistration.setAllowedOrigins(
                        any(String[].class)
                )
        ).thenReturn(endpointRegistration);

        lenient().when(endpointRegistration.withSockJS())
                .thenReturn(null); // method returns void in some Spring versions
    }

    @Test
    void registerStompEndpoints_registersWsEndpoint() {
        config.registerStompEndpoints(stompEndpointRegistry);

        verify(stompEndpointRegistry).addEndpoint("/ws");
        verify(endpointRegistration).setAllowedOrigins("http://localhost:5173", "http://localhost:3000");
        verify(endpointRegistration).withSockJS();
    }

    @Test
    void configureMessageBroker_registersBrokerAndPrefixes() {
        config.configureMessageBroker(messageBrokerRegistry);

        verify(messageBrokerRegistry).enableSimpleBroker("/topic");
        verify(messageBrokerRegistry).setApplicationDestinationPrefixes("/app");
    }
}
