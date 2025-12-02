package com.example.lab_signoff_backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorsConfigTest {

    @Mock
    private CorsRegistry registry;
    @Mock
    private CorsRegistration registration;

    @Test
    void corsConfigurer_appliesAllowedOriginsFromProperty() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://a.com,http://b.com");

        when(registry.addMapping("/**")).thenReturn(registration);
        lenient().when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
        lenient().when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
        lenient().when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);
        lenient().when(registration.allowCredentials(true)).thenReturn(registration);

        config.corsConfigurer().addCorsMappings(registry);

        verify(registry).addMapping("/**");
        verify(registration).allowedOrigins("http://a.com", "http://b.com");
        verify(registration).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        verify(registration).allowedHeaders("*");
        verify(registration).allowCredentials(true);
    }
}
