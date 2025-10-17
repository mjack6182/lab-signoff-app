package com.example.lab_signoff_backend.model;

import org.springframework.data.annotation.Id;
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
 * @version 1.0
 */
@Document(collection = "signoff_events")
public class SignoffEvent {

    /**
     * Unique identifier for the signoff event
     */
    @Id
    private String id;

    /**
     * Reference to the lab this event is associated with
     */
    private String labId;

    /**
     * Reference to the group this event is associated with
     */
    private String groupId;

    /**
     * Action performed: either "PASS" or "RETURN"
     */
    private String action;

    /**
     * Timestamp when the event occurred (ISO 8601 format)
     */
    private Instant timestamp;

    /**
     * Identifier of the instructor or TA who performed the action
     */
    private String performedBy;

    /**
     * Optional notes or comments about the action
     */
    private String notes;

    /**
     * Checkpoint number if applicable (optional, for future use)
     */
    private Integer checkpointNumber;

    /**
     * Default constructor for SignoffEvent
     * Required for Spring Data MongoDB serialization/deserialization
     */
    public SignoffEvent() {
    }

    /**
     * Constructor for creating a SignoffEvent with all information
     *
     * @param id                Unique identifier for the event
     * @param labId             Identifier of the lab
     * @param groupId           Identifier of the group
     * @param action            Action performed (PASS or RETURN)
     * @param timestamp         When the event occurred
     * @param performedBy       Who performed the action
     * @param notes             Optional notes
     * @param checkpointNumber  Optional checkpoint number
     */
    public SignoffEvent(String id, String labId, String groupId, String action,
                       Instant timestamp, String performedBy, String notes, Integer checkpointNumber) {
        this.id = id;
        this.labId = labId;
        this.groupId = groupId;
        this.action = action;
        this.timestamp = timestamp;
        this.performedBy = performedBy;
        this.notes = notes;
        this.checkpointNumber = checkpointNumber;
    }

    /**
     * Builder pattern constructor for creating a SignoffEvent with required fields
     *
     * @param labId       Identifier of the lab
     * @param groupId     Identifier of the group
     * @param action      Action performed (PASS or RETURN)
     * @param performedBy Who performed the action
     */
    public SignoffEvent(String labId, String groupId, String action, String performedBy) {
        this.labId = labId;
        this.groupId = groupId;
        this.action = action;
        this.timestamp = Instant.now();
        this.performedBy = performedBy;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
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

    @Override
    public String toString() {
        return "SignoffEvent{" +
                "id='" + id + '\'' +
                ", labId='" + labId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                ", performedBy='" + performedBy + '\'' +
                ", notes='" + notes + '\'' +
                ", checkpointNumber=" + checkpointNumber +
                '}';
    }
}
