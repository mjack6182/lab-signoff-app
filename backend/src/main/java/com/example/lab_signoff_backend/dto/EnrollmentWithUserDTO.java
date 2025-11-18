package com.example.lab_signoff_backend.dto;

import com.example.lab_signoff_backend.model.enums.EnrollmentRole;
import com.example.lab_signoff_backend.model.enums.EnrollmentStatus;

import java.time.Instant;

/**
 * Data Transfer Object combining Enrollment and User information
 * Used when returning student/TA rosters with user details
 */
public class EnrollmentWithUserDTO {
    // Enrollment fields
    private String id;
    private String userId;
    private String classId;
    private EnrollmentRole role;
    private EnrollmentStatus status;
    private Instant enrolledAt;
    private Instant updatedAt;

    // User fields
    private String userName;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private String userPicture;

    // Constructors
    public EnrollmentWithUserDTO() {
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public void setUserLastName(String userLastName) {
        this.userLastName = userLastName;
    }

    public String getUserPicture() {
        return userPicture;
    }

    public void setUserPicture(String userPicture) {
        this.userPicture = userPicture;
    }
}
