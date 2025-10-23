package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestBroadcastController {

    @Autowired
    private LabWebSocketController wsController;

    // Temporary GET endpoint to trigger broadcast
    @GetMapping("/test-broadcast")
    public String sendTest() {
        wsController.broadcastCheckpointUpdate("Group-1", 1, "PASS");
        return "Broadcast sent!";
    }
}