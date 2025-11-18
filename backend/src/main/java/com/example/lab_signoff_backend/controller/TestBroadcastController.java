package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.CheckpointUpdate;
import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestBroadcastController {

    @Autowired
    private LabWebSocketController wsController;

    @GetMapping("/test-broadcast")
    public String sendTest() {
        CheckpointUpdate testUpdate = new CheckpointUpdate("lab1", "Group-1", 1, "PASS");
        wsController.broadcastCheckpointUpdate("lab1", testUpdate);
        return "✅ Broadcast sent!";
    }
}