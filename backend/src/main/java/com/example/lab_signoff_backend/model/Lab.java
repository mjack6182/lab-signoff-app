package com.example.lab_signoff_backend.model;

import com.example.lab_signoff_backend.model.embedded.CheckpointDefinition;
import com.example.lab_signoff_backend.model.enums.LabStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Lab assignment within a Class
 * Contains checkpoints, join code, and configuration
 */
@Document(collection = "labs")
public class Lab {
    @Id
    private String id;

    @NotBlank(message = "Class ID is required")
    @Indexed
    private String classId;  // References Class._id

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Points are required")
    @Min(value = 1, message = "Points must be at least 1")
    private Integer points;  // Total points = number of checkpoints

    @NotBlank(message = "Join code is required")
    @Indexed(unique = true)
    private String joinCode;  // Unique 6-character code

    @NotNull(message = "Status is required")
    @Indexed
    private LabStatus status;

    private Instant startTime;  // When join code becomes active
    private Instant endTime;    // Deadline

    @Min(value = 1, message = "Max group size must be at least 1")
    private Integer maxGroupSize = 3;

    @Min(value = 1, message = "Min group size must be at least 1")
    private Integer minGroupSize = 2;

    private Boolean autoRandomize = true;  // Auto-create groups on lab start

    // Embedded checkpoint definitions (1 point = 1 checkpoint)
    private List<CheckpointDefinition> checkpoints = new ArrayList<>();

    @NotNull
    private Instant createdAt;

    private Instant updatedAt;

    @NotBlank(message = "Created by user ID is required")
    private String createdBy;  // References User._id

    // Constructors
    public Lab() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = LabStatus.DRAFT;
        this.joinCode = generateJoinCode();
    }

    public Lab(String classId, String title, Integer points, String createdBy) {
        this();
        this.classId = classId;
        this.title = title;
        this.points = points;
        this.createdBy = createdBy;
        initializeCheckpoints(points);
    }

    // Helper methods
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }

    /**
     * Generate a unique 6-character alphanumeric join code
     */
    public static String generateJoinCode() {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        return uuid.substring(0, 6).toUpperCase();
    }

    /**
     * Initialize checkpoints based on points (1 point = 1 checkpoint)
     */
    private void initializeCheckpoints(Integer totalPoints) {
        this.checkpoints.clear();
        for (int i = 1; i <= totalPoints; i++) {
            CheckpointDefinition checkpoint = new CheckpointDefinition();
            checkpoint.setNumber(i);
            checkpoint.setName("Checkpoint " + i);
            checkpoint.setDescription("Complete checkpoint " + i);
            checkpoint.setPoints(1);
            checkpoint.setRequired(true);
            this.checkpoints.add(checkpoint);
        }
    }

    /**
     * Regenerate join code (if needed)
     */
    public void regenerateJoinCode() {
        this.joinCode = generateJoinCode();
        updateTimestamp();
    }

    /**
     * Activate the lab (make join code active)
     */
    public void activate() {
        this.status = LabStatus.ACTIVE;
        this.startTime = Instant.now();
        updateTimestamp();
    }

    /**
     * Close the lab (no more submissions)
     */
    public void close() {
        this.status = LabStatus.CLOSED;
        this.endTime = Instant.now();
        updateTimestamp();
    }

    /**
     * Archive the lab
     */
    public void archive() {
        this.status = LabStatus.ARCHIVED;
        updateTimestamp();
    }

    public boolean isActive() {
        return this.status == LabStatus.ACTIVE;
    }

    public boolean isDraft() {
        return this.status == LabStatus.DRAFT;
    }

    public boolean isClosed() {
        return this.status == LabStatus.CLOSED;
    }

    public boolean isArchived() {
        return this.status == LabStatus.ARCHIVED;
    }

    public boolean isJoinable() {
        return isActive() && (endTime == null || Instant.now().isBefore(endTime));
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
        if (points != null && points > 0) {
            initializeCheckpoints(points);
        }
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public LabStatus getStatus() {
        return status;
    }

    public void setStatus(LabStatus status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Integer getMaxGroupSize() {
        return maxGroupSize;
    }

    public void setMaxGroupSize(Integer maxGroupSize) {
        this.maxGroupSize = maxGroupSize;
    }

    public Integer getMinGroupSize() {
        return minGroupSize;
    }

    public void setMinGroupSize(Integer minGroupSize) {
        this.minGroupSize = minGroupSize;
    }

    public Boolean getAutoRandomize() {
        return autoRandomize;
    }

    public void setAutoRandomize(Boolean autoRandomize) {
        this.autoRandomize = autoRandomize;
    }

    public List<CheckpointDefinition> getCheckpoints() {
        return checkpoints;
    }

    public void setCheckpoints(List<CheckpointDefinition> checkpoints) {
        this.checkpoints = checkpoints;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // Legacy compatibility (for existing code that uses courseId/lineItemId)
    @Deprecated
    public String getCourseId() {
        return this.classId;
    }

    @Deprecated
    public void setCourseId(String courseId) {
        this.classId = courseId;
    }

    @Deprecated
    public String getLineItemId() {
        // This could be stored in Class.canvasMetadata instead
        return null;
    }
}
