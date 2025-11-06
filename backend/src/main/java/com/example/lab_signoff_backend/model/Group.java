package com.example.lab_signoff_backend.model;

import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Group model class representing a student group for a lab assignment.
 * This class is mapped to the "groups" collection in MongoDB.
 *
 * Groups contain information about students working together on labs,
 * their progress, and signoff status.
 */
@Document(collection = "groups")
public class Group {
    /**
     * Unique identifier for the group
     */
    @Id
    private String id;

    /**
     * Custom group identifier (e.g., "Group-1", "Team-A")
     */
    @NotBlank(message = "Group name is required")
    private String groupId;

    /**
     * Reference to the lab this group is associated with
     */
    @NotBlank(message = "Lab ID is required")
    @Indexed
    private String labId;

    /**
     * Numeric identifier for sorting
     */
    private Integer groupNumber;

    /**
     * List of group members (embedded documents)
     */
    private List<GroupMember> members = new ArrayList<>();

    /**
     * Current status of the group
     */
    @NotNull(message = "Status is required")
    @Indexed
    private GroupStatus status;

    /**
     * Checkpoint progress tracking (embedded)
     */
    private List<CheckpointProgress> checkpointProgress = new ArrayList<>();

    private Integer currentCheckpoint = 1;

    private BigDecimal totalScore = BigDecimal.ZERO;

    private BigDecimal finalGrade = BigDecimal.ZERO;

    @NotNull
    private Instant createdAt;

    private Instant lastUpdatedAt;

    private Instant completedAt;

    private Integer generationNumber = 1;

    /**
     * Default constructor for Group
     * Required for Spring Data MongoDB serialization/deserialization
     */
    public Group() {
        this.createdAt = Instant.now();
        this.lastUpdatedAt = Instant.now();
        this.status = GroupStatus.FORMING;
    }

    /**
     * Constructor for creating a Group with all information (BACKWARD COMPATIBLE)
     *
     * @param id      Unique identifier for the group
     * @param groupId Custom group identifier
     * @param labId   Identifier of the lab this group belongs to
     * @param members List of member identifiers (legacy string format)
     * @param status  Current status of the group (legacy string format)
     */
    @Deprecated
    public Group(String id, String groupId, String labId, List<String> members, String status) {
        this();
        this.id = id;
        this.groupId = groupId;
        this.labId = labId;
        // Convert legacy string members to GroupMember objects
        for (String memberId : members) {
            GroupMember member = new GroupMember();
            member.setUserId(memberId);
            member.setName(memberId);
            member.setEmail(memberId);
            this.members.add(member);
        }
        // Convert legacy string status to enum
        try {
            this.status = GroupStatus.valueOf(status.toUpperCase().replace("-", "_"));
        } catch (Exception e) {
            this.status = GroupStatus.IN_PROGRESS;
        }
    }

    // Helper methods
    public void updateTimestamp() {
        this.lastUpdatedAt = Instant.now();
    }

    public void addMember(GroupMember member) {
        if (!this.members.contains(member)) {
            this.members.add(member);
            updateTimestamp();
        }
    }

    public void removeMember(String userId) {
        this.members.removeIf(m -> m.getUserId().equals(userId));
        updateTimestamp();
    }

    public void startProgress() {
        this.status = GroupStatus.IN_PROGRESS;
        updateTimestamp();
    }

    public void complete() {
        this.status = GroupStatus.COMPLETED;
        this.completedAt = Instant.now();
        updateTimestamp();
    }

    public void signOff() {
        this.status = GroupStatus.SIGNED_OFF;
        updateTimestamp();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getLabId() {
        return labId;
    }

    public void setLabId(String labId) {
        this.labId = labId;
    }

    public Integer getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(Integer groupNumber) {
        this.groupNumber = groupNumber;
    }

    public List<GroupMember> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMember> members) {
        this.members = members;
    }

    public GroupStatus getStatus() {
        return status;
    }

    public void setStatus(GroupStatus status) {
        this.status = status;
    }

    public List<CheckpointProgress> getCheckpointProgress() {
        return checkpointProgress;
    }

    public void setCheckpointProgress(List<CheckpointProgress> checkpointProgress) {
        this.checkpointProgress = checkpointProgress;
    }

    public Integer getCurrentCheckpoint() {
        return currentCheckpoint;
    }

    public void setCurrentCheckpoint(Integer currentCheckpoint) {
        this.currentCheckpoint = currentCheckpoint;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public BigDecimal getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(BigDecimal finalGrade) {
        this.finalGrade = finalGrade;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getGenerationNumber() {
        return generationNumber;
    }

    public void setGenerationNumber(Integer generationNumber) {
        this.generationNumber = generationNumber;
    }
}
