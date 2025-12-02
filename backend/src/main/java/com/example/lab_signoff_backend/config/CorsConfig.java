package com.example.lab_signoff_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    // ✅ This bean defines the CORS (Cross-Origin Resource Sharing) rules for the entire backend.
    // It allows the frontend (running locally, hosted on Firebase, or deployed on Railway) to communicate with the backend API.
    // Allowed origins are configured via the ALLOWED_ORIGINS environment variable (comma-separated).
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @SuppressWarnings("null")
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = allowedOrigins.split(",");
                registry.addMapping("/**") // Apply to all API endpoints
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow main HTTP verbs
                        .allowedHeaders("*") // Allow all headers (Authorization, Content-Type, etc.)
                        .allowCredentials(true); // Allow cookies or credentials in cross-origin requests
            }
        };
    }
}