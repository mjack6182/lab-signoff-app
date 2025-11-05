package com.example.lab_signoff_backend.model;

import java.time.Instant;

/**
 * WebSocket DTO for checkpoint update events
 * Sent when a checkpoint is signed off or returned
 */
public class CheckpointUpdate {
    private String labId;
    private String groupId;
    private Integer checkpointNumber;
    private String status; // "PASS" or "RETURN"
    private String signedOffBy;
    private String signedOffByName;
    private Instant timestamp;
    private String notes;
    private Integer pointsAwarded;

    public CheckpointUpdate() {}

    public CheckpointUpdate(String labId, String groupId, Integer checkpointNumber, String status) {
        this.labId = labId;
        this.groupId = groupId;
        this.checkpointNumber = checkpointNumber;
        this.status = status;
        this.timestamp = Instant.now();
    }

    // Getters and setters
    public String getLabId() { return labId; }
    public void setLabId(String labId) { this.labId = labId; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public Integer getCheckpointNumber() { return checkpointNumber; }
    public void setCheckpointNumber(Integer checkpointNumber) { this.checkpointNumber = checkpointNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSignedOffBy() { return signedOffBy; }
    public void setSignedOffBy(String signedOffBy) { this.signedOffBy = signedOffBy; }

    public String getSignedOffByName() { return signedOffByName; }
    public void setSignedOffByName(String signedOffByName) { this.signedOffByName = signedOffByName; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(Integer pointsAwarded) { this.pointsAwarded = pointsAwarded; }
}
