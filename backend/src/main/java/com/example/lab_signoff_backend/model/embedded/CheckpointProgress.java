package com.example.lab_signoff_backend.model.embedded;

import com.example.lab_signoff_backend.model.enums.SignoffAction;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Embedded document representing progress on a specific checkpoint
 */
public class CheckpointProgress {
    @NotNull(message = "Checkpoint number is required")
    private Integer checkpointNumber;

    @NotNull(message = "Status is required")
    private SignoffAction status;

    private String signedOffBy;  // User ID of TA/Teacher
    private String signedOffByName;  // Name for display
    private Instant timestamp;
    private String notes;
    private Integer pointsAwarded;

    // Constructors
    public CheckpointProgress() {
    }

    public CheckpointProgress(Integer checkpointNumber, SignoffAction status) {
        this.checkpointNumber = checkpointNumber;
        this.status = status;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public Integer getCheckpointNumber() {
        return checkpointNumber;
    }

    public void setCheckpointNumber(Integer checkpointNumber) {
        this.checkpointNumber = checkpointNumber;
    }

    public SignoffAction getStatus() {
        return status;
    }

    public void setStatus(SignoffAction status) {
        this.status = status;
    }

    public String getSignedOffBy() {
        return signedOffBy;
    }

    public void setSignedOffBy(String signedOffBy) {
        this.signedOffBy = signedOffBy;
    }

    public String getSignedOffByName() {
        return signedOffByName;
    }

    public void setSignedOffByName(String signedOffByName) {
        this.signedOffByName = signedOffByName;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(Integer pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }
}
