package com.example.lab_signoff_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    // ✅ This bean defines the CORS (Cross-Origin Resource Sharing) rules for the entire backend.
    // It allows the frontend (running locally or hosted on Firebase) to communicate with the backend API.
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Apply to all API endpoints
                        .allowedOrigins(
                                "http://localhost:5173",   // React/Vite local dev environment
                                "http://localhost:5002",   // Firebase local hosting emulator
                                "https://lab-signoff-app.web.app",   // Firebase live hosting
                                "https://lab-signoff-app.firebaseapp.com" // Alternate Firebase domain
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow main HTTP verbs
                        .allowedHeaders("*") // Allow all headers (Authorization, Content-Type, etc.)
                        .allowCredentials(true); // Allow cookies or credentials in cross-origin requests
            }
        };
    }
}