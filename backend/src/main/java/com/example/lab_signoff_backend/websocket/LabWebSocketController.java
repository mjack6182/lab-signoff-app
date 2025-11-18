package com.example.lab_signoff_backend.websocket;

import com.example.lab_signoff_backend.model.CheckpointUpdate;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import com.example.lab_signoff_backend.model.websocket.GroupStatusUpdate;
import com.example.lab_signoff_backend.model.websocket.HelpQueueUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LabWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(LabWebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast checkpoint update to lab & group topics
     */
    public void broadcastCheckpointUpdate(String labId, CheckpointUpdate update) {
    // only send to the main UI topic
    messagingTemplate.convertAndSend("/topic/group-updates", update);

    logger.info("Broadcasted checkpoint update -> Lab: {}, Group: {}, Checkpoint: {}, Status: {}",
            labId, update.getGroupId(), update.getCheckpointNumber(), update.getStatus());
    }

    /**
     * Broadcast group passed status
     */
    public void broadcastGroupPassed(String labId, String groupId) {
        GroupStatusUpdate update = new GroupStatusUpdate(labId, groupId, null);
        update.setStatus(GroupStatus.SIGNED_OFF);
        // topic specific to group
        messagingTemplate.convertAndSend("/topic/group-updates/" + groupId, update);
        // also send to generic feed so clients listening to the generic channel get notified
        messagingTemplate.convertAndSend("/topic/group-updates", update);

        logger.info("Broadcasted group passed -> Lab: {}, Group: {}", labId, groupId);
    }

    /**
     * Broadcast group status update
     */
    public void broadcastGroupStatusUpdate(String labId, GroupStatusUpdate update) {
        messagingTemplate.convertAndSend("/topic/labs/" + labId + "/groups", update);
        messagingTemplate.convertAndSend("/topic/groups/" + update.getGroupId() + "/status", update);

        logger.info("Broadcasted group status update -> Lab: {}, Group: {}, Status: {}",
                labId, update.getGroupId(), update.getStatus());
    }

    /**
     * Broadcast help queue update
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
     * Broadcast groups randomized event to all clients subscribed to the lab
     * @param labId The lab identifier
     */
    public void broadcastGroupsRandomized(String labId) {
        messagingTemplate.convertAndSend("/topic/labs/" + labId + "/groups-randomized",
            new GroupsRandomizedMessage(labId));
        logger.info("Broadcasted groups randomized event -> Lab: {}", labId);
    }

    /**
     * Test endpoint to verify WebSocket connectivity
     */
    @GetMapping("/ws-test-broadcast")
    @ResponseBody
    public String testBroadcast() {
        CheckpointUpdate testUpdate = new CheckpointUpdate("test-lab-123", "test-group-123", 1, "PASS");
        broadcastCheckpointUpdate("test-lab-123", testUpdate);
        return "✅ Test WebSocket broadcast sent!";
    }

    /**
     * Message class for groups randomized events
     */
    public static class GroupsRandomizedMessage {
        private final String labId;
        private final long timestamp;
        private final String message;

        public GroupsRandomizedMessage(String labId) {
            this.labId = labId;
            this.timestamp = System.currentTimeMillis();
            this.message = "Groups have been randomized";
        }

        public String getLabId() { return labId; }
        public long getTimestamp() { return timestamp; }
        public String getMessage() { return message; }
    }
}