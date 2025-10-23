package com.example.lab_signoff_backend.websocket;

import com.example.lab_signoff_backend.model.CheckpointUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LabWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Simple test message endpoint for WebSocket
    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public String greeting(String message) {
        return "Hello from backend, you sent: " + message;
    }

    // Broadcast checkpoint updates
    public void broadcastCheckpointUpdate(String groupId, int checkpointNumber, String status) {
        CheckpointUpdate update = new CheckpointUpdate(groupId, checkpointNumber, status);
        messagingTemplate.convertAndSend("/topic/group-updates", update);

        // Log broadcast for testing
        System.out.println("Broadcasted update: " + groupId + " | Checkpoint: " + checkpointNumber + " | Status: " + status);
    }

    // Updated HTTP endpoint with unique path to avoid conflict
    @GetMapping("/ws-test-broadcast")
    public String testBroadcast() {
        // Sends a real-time WebSocket update to everyone listening on the frontend.
        // Simulating that "Group-1" has passed checkpoint 1.
        broadcastCheckpointUpdate("Group-1", 1, "PASS");

        // Returns a confirmation message to whoever triggered this endpoint
        return "Broadcast sent!";
    }
}