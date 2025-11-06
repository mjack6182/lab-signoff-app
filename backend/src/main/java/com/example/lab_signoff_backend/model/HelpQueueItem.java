package com.example.lab_signoff_backend.model;

import com.example.lab_signoff_backend.model.enums.HelpQueuePriority;
import com.example.lab_signoff_backend.model.enums.HelpQueueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Represents a help queue item (hands raised) for a lab
 * Per-lab help queue for students requesting assistance
 */
@Document(collection = "help_queue_items")
@CompoundIndexes({
    @CompoundIndex(name = "lab_status_idx", def = "{'labId': 1, 'status': 1}"),
    @CompoundIndex(name = "lab_position_idx", def = "{'labId': 1, 'position': 1}")
})
public class HelpQueueItem {
    @Id
    private String id;

    @NotBlank(message = "Lab ID is required")
    @Indexed
    private String labId;  // References Lab._id

    @NotBlank(message = "Group ID is required")
    @Indexed
    private String groupId;  // References Group._id

    @NotBlank(message = "Raised by user ID is required")
    private String raisedBy;  // References User._id (student who raised hand)

    @NotNull(message = "Status is required")
    private HelpQueueStatus status;

    private String description;  // Optional help request details

    @NotNull
    private Instant raisedAt;

    private String claimedBy;  // References User._id (TA/Teacher)
    private Instant claimedAt;
    private Instant resolvedAt;

    @NotNull
    private Integer position;  // Position in queue

    @NotNull
    private HelpQueuePriority priority;

    // Constructors
    public HelpQueueItem() {
        this.raisedAt = Instant.now();
        this.status = HelpQueueStatus.WAITING;
        this.priority = HelpQueuePriority.NORMAL;
    }

    public HelpQueueItem(String labId, String groupId, String raisedBy, Integer position) {
        this();
        this.labId = labId;
        this.groupId = groupId;
        this.raisedBy = raisedBy;
        this.position = position;
    }

    // Helper methods
    public void claim(String userId) {
        this.status = HelpQueueStatus.CLAIMED;
        this.claimedBy = userId;
        this.claimedAt = Instant.now();
    }

    public void resolve() {
        this.status = HelpQueueStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public void cancel() {
        this.status = HelpQueueStatus.CANCELLED;
        this.resolvedAt = Instant.now();
    }

    public boolean isWaiting() {
        return this.status == HelpQueueStatus.WAITING;
    }

    public boolean isClaimed() {
        return this.status == HelpQueueStatus.CLAIMED;
    }

    public boolean isResolved() {
        return this.status == HelpQueueStatus.RESOLVED;
    }

    public boolean isCancelled() {
        return this.status == HelpQueueStatus.CANCELLED;
    }

    public boolean isActive() {
        return isWaiting() || isClaimed();
    }

    public void setUrgent() {
        this.priority = HelpQueuePriority.URGENT;
    }

    public void setNormal() {
        this.priority = HelpQueuePriority.NORMAL;
    }

    public boolean isUrgent() {
        return this.priority == HelpQueuePriority.URGENT;
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

    public String getRaisedBy() {
        return raisedBy;
    }

    public void setRaisedBy(String raisedBy) {
        this.raisedBy = raisedBy;
    }

    public HelpQueueStatus getStatus() {
        return status;
    }

    public void setStatus(HelpQueueStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getRaisedAt() {
        return raisedAt;
    }

    public void setRaisedAt(Instant raisedAt) {
        this.raisedAt = raisedAt;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public HelpQueuePriority getPriority() {
        return priority;
    }

    public void setPriority(HelpQueuePriority priority) {
        this.priority = priority;
    }
}
