package com.example.lab_signoff_backend.model.websocket;

import com.example.lab_signoff_backend.model.enums.HelpQueueStatus;
import com.example.lab_signoff_backend.model.enums.HelpQueuePriority;
import java.time.Instant;

/**
 * WebSocket DTO for help queue update events
 * Sent when help requests are raised, claimed, or resolved
 */
public class HelpQueueUpdate {
    private String id;
    private String labId;
    private String groupId;
    private HelpQueueStatus status;
    private HelpQueueStatus previousStatus;
    private HelpQueuePriority priority;
    private Integer position;
    private String requestedBy;
    private String requestedByName;
    private String claimedBy;
    private String claimedByName;
    private Instant timestamp;
    private String description;

    public HelpQueueUpdate() {
        this.timestamp = Instant.now();
    }

    public HelpQueueUpdate(String id, String labId, String groupId, HelpQueueStatus status) {
        this();
        this.id = id;
        this.labId = labId;
        this.groupId = groupId;
        this.status = status;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabId() { return labId; }
    public void setLabId(String labId) { this.labId = labId; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public HelpQueueStatus getStatus() { return status; }
    public void setStatus(HelpQueueStatus status) { this.status = status; }

    public HelpQueueStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(HelpQueueStatus previousStatus) { this.previousStatus = previousStatus; }

    public HelpQueuePriority getPriority() { return priority; }
    public void setPriority(HelpQueuePriority priority) { this.priority = priority; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public String getRequestedByName() { return requestedByName; }
    public void setRequestedByName(String requestedByName) { this.requestedByName = requestedByName; }

    public String getClaimedBy() { return claimedBy; }
    public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }

    public String getClaimedByName() { return claimedByName; }
    public void setClaimedByName(String claimedByName) { this.claimedByName = claimedByName; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
