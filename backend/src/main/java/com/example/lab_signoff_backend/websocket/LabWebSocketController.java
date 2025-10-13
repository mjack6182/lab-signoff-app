package com.example.lab_signoff_backend.websocket;

import com.example.lab_signoff_backend.model.CheckpointUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class LabWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Simple test message endpoint
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
}
