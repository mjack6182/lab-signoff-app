package com.example.lab_signoff_backend.model.websocket;

import com.example.lab_signoff_backend.model.enums.GroupStatus;
import java.time.Instant;

/**
 * WebSocket DTO for group status change events
 * Sent when a group's overall status changes (FORMING, IN_PROGRESS, COMPLETED, SIGNED_OFF)
 */
public class GroupStatusUpdate {
    private String labId;
    private String groupId;
    private GroupStatus status;
    private GroupStatus previousStatus;
    private Instant timestamp;
    private String performedBy;
    private String performedByName;
    private Integer totalScore;
    private String finalGrade;

    public GroupStatusUpdate() {
        this.timestamp = Instant.now();
    }

    public GroupStatusUpdate(String labId, String groupId, GroupStatus status) {
        this();
        this.labId = labId;
        this.groupId = groupId;
        this.status = status;
    }

    // Getters and setters
    public String getLabId() { return labId; }
    public void setLabId(String labId) { this.labId = labId; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public GroupStatus getStatus() { return status; }
    public void setStatus(GroupStatus status) { this.status = status; }

    public GroupStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(GroupStatus previousStatus) { this.previousStatus = previousStatus; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getPerformedByName() { return performedByName; }
    public void setPerformedByName(String performedByName) { this.performedByName = performedByName; }

    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }

    public String getFinalGrade() { return finalGrade; }
    public void setFinalGrade(String finalGrade) { this.finalGrade = finalGrade; }
}
