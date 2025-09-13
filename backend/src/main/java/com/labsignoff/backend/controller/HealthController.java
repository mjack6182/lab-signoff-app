package com.labsignoff.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = {"http://localhost:3000"})
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "service", "lab-signoff-backend",
                "version", "0.0.1-SNAPSHOT"
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "application", Map.of(
                        "name", "Lab Signoff Backend",
                        "description", "Backend service for Lab Signoff Application",
                        "version", "0.0.1-SNAPSHOT"
                ),
                "features", Map.of(
                        "lti", "LTI 1.3 Integration",
                        "websockets", "Real-time Communication",
                        "database", "MongoDB",
                        "security", "OAuth2 + Spring Security"
                )
        ));
    }
}