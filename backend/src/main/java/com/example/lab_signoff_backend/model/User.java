package com.example.lab_signoff_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * User model class representing application users.
 * This class is mapped to the "users" collection in MongoDB.
 *
 * Users are synchronized from Auth0 upon login and contain
 * profile information and role assignments.
 */
@Document(collection = "users")
public class User {
    /**
     * Unique identifier for the user (MongoDB ObjectId)
     */
    @Id
    private String id;

    /**
     * Auth0 user ID (subject claim from JWT)
     * This is indexed for fast lookups and should be unique
     */
    @Indexed(unique = true)
    private String auth0Id;

    /**
     * User's email address
     */
    @Indexed
    private String email;

    /**
     * User's full name
     */
    private String name;

    /**
     * User's first name
     */
    private String firstName;

    /**
     * User's last name
     */
    private String lastName;

    /**
     * User's profile picture URL from Auth0
     */
    private String picture;

    /**
     * List of roles assigned to the user
     * Possible values: "Teacher", "TA", "Student"
     */
    private List<String> roles;

    /**
     * Primary role used for quick access control checks
     * This is typically the first role in the roles list
     */
    private String primaryRole;

    /**
     * Timestamp when the user was first created in the system
     */
    private Instant createdAt;

    /**
     * Timestamp of the user's last login
     */
    private Instant lastLogin;

    /**
     * Additional metadata from Auth0 (optional)
     */
    private String nickname;

    /**
     * Default constructor for User
     * Required for Spring Data MongoDB serialization/deserialization
     */
    public User() {
    }

    /**
     * Constructor for creating a User with basic Auth0 information
     *
     * @param auth0Id Auth0 user ID (sub claim)
     * @param email   User's email
     * @param name    User's full name
     * @param picture Profile picture URL
     * @param roles   List of assigned roles
     */
    public User(String auth0Id, String email, String name, String picture, List<String> roles) {
        this.auth0Id = auth0Id;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.roles = roles;
        this.primaryRole = (roles != null && !roles.isEmpty()) ? roles.get(0) : "Teacher";
        this.createdAt = Instant.now();
        this.lastLogin = Instant.now();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuth0Id() {
        return auth0Id;
    }

    public void setAuth0Id(String auth0Id) {
        this.auth0Id = auth0Id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
        // Update primary role when roles change
        this.primaryRole = (roles != null && !roles.isEmpty()) ? roles.get(0) : "Teacher";
    }

    public String getPrimaryRole() {
        return primaryRole;
    }

    public void setPrimaryRole(String primaryRole) {
        this.primaryRole = primaryRole;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Helper method to check if user has a specific role
     *
     * @param role Role to check
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Helper method to check if user is a teacher
     *
     * @return true if user has Teacher role
     */
    public boolean isTeacher() {
        return hasRole("Teacher");
    }

    /**
     * Helper method to check if user is a TA
     *
     * @return true if user has TA role
     */
    public boolean isTA() {
        return hasRole("TA");
    }

    /**
     * Helper method to check if user is a student
     *
     * @return true if user has Student role
     */
    public boolean isStudent() {
        return hasRole("Student");
    }

    /**
     * Helper method to check if user is an admin
     *
     * @return true if user has Admin role
     */
    public boolean isAdmin() {
        return hasRole("Admin");
    }

    /**
     * Helper method to check if user is staff (Teacher or TA)
     *
     * @return true if user has Teacher or TA role
     */
    public boolean isStaff() {
        return isTeacher() || isTA();
    }

    /**
     * Helper method to check if user is staff or admin (Teacher, TA, or Admin)
     *
     * @return true if user has Teacher, TA, or Admin role
     */
    public boolean isStaffOrAdmin() {
        return isTeacher() || isTA() || isAdmin();
    }

    /**
     * Update last login timestamp to current time
     */
    public void updateLastLogin() {
        this.lastLogin = Instant.now();
    }
}
