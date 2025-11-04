package com.example.lab_signoff_backend.websocket;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.lab_signoff_backend.model.CheckpointUpdate;

@Controller
public class LabWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(LabWebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public String greeting(String message) {
        logger.info("Received STOMP message: {}", message);
        return "Hello from backend, you sent: " + message;
    }

    public void broadcastCheckpointUpdate(String groupId, int checkpointNumber, String status) {
        CheckpointUpdate update = new CheckpointUpdate(groupId, checkpointNumber, status);
        messagingTemplate.convertAndSend("/topic/group-updates", update);
        logger.info("Broadcasted checkpoint update -> Group: {}, Checkpoint: {}, Status: {}", groupId, checkpointNumber, status);
    }

    public void broadcastGroupPassed(String groupId) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("groupId", groupId);
    payload.put("status", "GROUP_PASSED");

    messagingTemplate.convertAndSend("/topic/group-updates", payload);
    logger.info("🎓 Broadcasted GROUP PASSED event for group {}", groupId);
}

    @GetMapping("/ws-test-broadcast")
    @ResponseBody // ✅ ADD THIS LINE
    public String testBroadcast() {
        broadcastCheckpointUpdate("Group-1", 1, "PASS");
        return "✅ Test WebSocket broadcast sent to /topic/group-updates";
    }
}