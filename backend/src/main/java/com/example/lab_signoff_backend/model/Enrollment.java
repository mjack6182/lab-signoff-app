package com.example.lab_signoff_backend.model;

import com.example.lab_signoff_backend.model.enums.EnrollmentRole;
import com.example.lab_signoff_backend.model.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Represents an enrollment relationship between a User and a Class
 */
@Document(collection = "enrollments")
@CompoundIndexes({
    @CompoundIndex(name = "class_role_idx", def = "{'classId': 1, 'role': 1}"),
    @CompoundIndex(name = "user_class_idx", def = "{'userId': 1, 'classId': 1}", unique = true)
})
public class Enrollment {
    @Id
    private String id;

    @NotBlank(message = "User ID is required")
    @Indexed
    private String userId;  // References User._id

    @NotBlank(message = "Class ID is required")
    @Indexed
    private String classId;  // References Class._id

    @NotNull(message = "Role is required")
    private EnrollmentRole role;

    @NotNull(message = "Status is required")
    private EnrollmentStatus status;

    @NotNull
    private Instant enrolledAt;

    private Instant updatedAt;

    // For TA upgrade requests - stores teacher userId who performed upgrade
    private String upgradeRequestedBy;

    // Constructors
    public Enrollment() {
        this.enrolledAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = EnrollmentStatus.ACTIVE;
    }

    public Enrollment(String userId, String classId, EnrollmentRole role) {
        this();
        this.userId = userId;
        this.classId = classId;
        this.role = role;
    }

    // Helper methods
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }

    public void upgradeToTA(String performedBy) {
        this.role = EnrollmentRole.TA;
        this.upgradeRequestedBy = performedBy;
        updateTimestamp();
    }

    public void setRoleAndUpdate(EnrollmentRole newRole, String performedBy) {
        this.role = newRole;
        if (newRole == EnrollmentRole.TA) {
            this.upgradeRequestedBy = performedBy;
        }
        updateTimestamp();
    }

    public void drop() {
        this.status = EnrollmentStatus.DROPPED;
        updateTimestamp();
    }

    public void complete() {
        this.status = EnrollmentStatus.COMPLETED;
        updateTimestamp();
    }

    public boolean isActive() {
        return this.status == EnrollmentStatus.ACTIVE;
    }

    public boolean isStudent() {
        return this.role == EnrollmentRole.STUDENT;
    }

    public boolean isTA() {
        return this.role == EnrollmentRole.TA;
    }

    public boolean isTeacher() {
        return this.role == EnrollmentRole.TEACHER;
    }

    public boolean isStaff() {
        return isTeacher() || isTA();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public EnrollmentRole getRole() {
        return role;
    }

    public void setRole(EnrollmentRole role) {
        this.role = role;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(Instant enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpgradeRequestedBy() {
        return upgradeRequestedBy;
    }

    public void setUpgradeRequestedBy(String upgradeRequestedBy) {
        this.upgradeRequestedBy = upgradeRequestedBy;
    }
}
