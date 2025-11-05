package com.example.lab_signoff_backend.model;

import com.example.lab_signoff_backend.model.enums.SignoffAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * SignoffEvent model class representing an audit entry for lab signoff actions.
 * This class is mapped to the "signoff_events" collection in MongoDB.
 *
 * SignoffEvents track all pass/return actions performed on groups during lab sessions,
 * providing a complete audit trail for accountability and analysis.
 *
 * @author Lab Signoff App Team
 * @version 2.0
 */
@Document(collection = "signoff_events")
@CompoundIndexes({
    @CompoundIndex(name = "lab_timestamp_idx", def = "{'labId': 1, 'timestamp': 1}")
})
public class SignoffEvent {

    /**
     * Unique identifier for the signoff event
     */
    @Id
    private String id;

    /**
     * Reference to the lab this event is associated with
     */
    @NotBlank(message = "Lab ID is required")
    @Indexed
    private String labId;

    /**
     * Reference to the group this event is associated with
     */
    @NotBlank(message = "Group ID is required")
    @Indexed
    private String groupId;

    /**
     * Action performed: PASS, RETURN, or COMPLETE
     */
    @NotNull(message = "Action is required")
    private SignoffAction action;

    /**
     * Timestamp when the event occurred (ISO 8601 format)
     */
    @NotNull
    private Instant timestamp;

    /**
     * Identifier of the instructor or TA who performed the action
     */
    @NotBlank(message = "Performed by is required")
    private String performedBy;

    /**
     * Role of the person who performed the action (TA or Teacher)
     */
    private String performerRole;

    /**
     * Optional notes or comments about the action
     */
    private String notes;

    /**
     * Checkpoint number (required in new schema)
     */
    @NotNull(message = "Checkpoint number is required")
    private Integer checkpointNumber;

    /**
     * Points awarded for this checkpoint
     */
    private Integer pointsAwarded;

    /**
     * Default constructor for SignoffEvent
     * Required for Spring Data MongoDB serialization/deserialization
     */
    public SignoffEvent() {
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabId() {
        return labId;
    }

    public void setLabId(String labId) {
        this.labId = labId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public SignoffAction getAction() {
        return action;
    }

    public void setAction(SignoffAction action) {
        this.action = action;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getPerformerRole() {
        return performerRole;
    }

    public void setPerformerRole(String performerRole) {
        this.performerRole = performerRole;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getCheckpointNumber() {
        return checkpointNumber;
    }

    public void setCheckpointNumber(Integer checkpointNumber) {
        this.checkpointNumber = checkpointNumber;
    }

    public Integer getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(Integer pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    @Override
    public String toString() {
        return "SignoffEvent{" +
                "id='" + id + '\'' +
                ", labId='" + labId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", action=" + action +
                ", timestamp=" + timestamp +
                ", performedBy='" + performedBy + '\'' +
                ", performerRole='" + performerRole + '\'' +
                ", notes='" + notes + '\'' +
                ", checkpointNumber=" + checkpointNumber +
                ", pointsAwarded=" + pointsAwarded +
                '}';
    }
}
