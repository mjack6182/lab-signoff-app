package com.example.lab_signoff_backend.config;

import com.example.lab_signoff_backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void corsConfigurationSource_parsesAllowedOrigins() throws Exception {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://a.com,http://b.com");

        CorsConfigurationSource source = config.corsConfigurationSource();
        var cors = source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());
        assertNotNull(cors);
        assertEquals(2, cors.getAllowedOrigins().size());
        assertTrue(cors.getAllowedMethods().contains("POST"));
    }
}
