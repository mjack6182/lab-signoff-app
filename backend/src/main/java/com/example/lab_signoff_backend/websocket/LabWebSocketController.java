package com.example.lab_signoff_backend.websocket;

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
import com.example.lab_signoff_backend.model.websocket.GroupStatusUpdate;
import com.example.lab_signoff_backend.model.websocket.HelpQueueUpdate;

/**
 * WebSocket controller for real-time updates
 * Handles broadcasting events to connected clients
 */
@Controller
public class LabWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(LabWebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Test endpoint for STOMP messaging
     */
    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public String greeting(String message) {
        logger.info("Received STOMP message: {}", message);
        return "Hello from backend, you sent: " + message;
    }

    /**
     * Broadcast checkpoint update to all clients subscribed to the lab
     * @param labId The lab identifier
     * @param update The checkpoint update details
     */
    public void broadcastCheckpointUpdate(String labId, CheckpointUpdate update) {
        messagingTemplate.convertAndSend("/topic/labs/" + labId + "/checkpoints", update);
        messagingTemplate.convertAndSend("/topic/groups/" + update.getGroupId() + "/checkpoints", update);
        logger.info("Broadcasted checkpoint update -> Lab: {}, Group: {}, Checkpoint: {}, Status: {}",
            labId, update.getGroupId(), update.getCheckpointNumber(), update.getStatus());
    }

    /**
     * Broadcast group status update to all clients subscribed to the lab
     * @param labId The lab identifier
     * @param update The group status update details
     */
    public void broadcastGroupStatusUpdate(String labId, GroupStatusUpdate update) {
        messagingTemplate.convertAndSend("/topic/labs/" + labId + "/groups", update);
        messagingTemplate.convertAndSend("/topic/groups/" + update.getGroupId() + "/status", update);
        logger.info("Broadcasted group status update -> Lab: {}, Group: {}, Status: {}",
            labId, update.getGroupId(), update.getStatus());
    }

    /**
     * Broadcast help queue update to all clients subscribed to the lab
     * @param labId The lab identifier
     * @param update The help queue update details
     */
    public void broadcastHelpQueueUpdate(String labId, HelpQueueUpdate update) {
        messagingTemplate.convertAndSend("/topic/labs/" + labId + "/help-queue", update);
        if (update.getGroupId() != null) {
            messagingTemplate.convertAndSend("/topic/groups/" + update.getGroupId() + "/help-queue", update);
        }
        logger.info("Broadcasted help queue update -> Lab: {}, Queue Item: {}, Status: {}",
            labId, update.getId(), update.getStatus());
    }

    /**
     * Legacy method - kept for backward compatibility
     * @deprecated Use broadcastCheckpointUpdate with CheckpointUpdate object instead
     */
    @Deprecated
    public void broadcastCheckpointUpdate(String groupId, int checkpointNumber, String status) {
        CheckpointUpdate update = new CheckpointUpdate(null, groupId, checkpointNumber, status);
        messagingTemplate.convertAndSend("/topic/group-updates", update);
        logger.warn("Using deprecated broadcastCheckpointUpdate method - missing labId");
    }

    /**
     * Legacy method - kept for backward compatibility
     * @deprecated Use broadcastGroupStatusUpdate instead
     */
    @Deprecated
    public void broadcastGroupPassed(String groupId) {
        GroupStatusUpdate update = new GroupStatusUpdate(null, groupId, null);
        update.setStatus(com.example.lab_signoff_backend.model.enums.GroupStatus.SIGNED_OFF);
        messagingTemplate.convertAndSend("/topic/group-updates", update);
        logger.warn("Using deprecated broadcastGroupPassed method - missing labId");
    }

    /**
     * Test endpoint to verify WebSocket connectivity
     */
    @GetMapping("/ws-test-broadcast")
    @ResponseBody
    public String testBroadcast() {
        CheckpointUpdate testUpdate = new CheckpointUpdate("test-lab-123", "test-group-123", 1, "PASS");
        testUpdate.setSignedOffByName("Test Teacher");
        testUpdate.setPointsAwarded(1);
        broadcastCheckpointUpdate("test-lab-123", testUpdate);
        return "✅ Test WebSocket broadcast sent to /topic/labs/test-lab-123/checkpoints";
    }
}