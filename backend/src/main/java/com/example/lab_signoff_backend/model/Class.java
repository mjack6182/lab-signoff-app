package com.example.lab_signoff_backend.model;

import com.example.lab_signoff_backend.model.embedded.CanvasMetadata;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Class/Course in the system
 * Imported from Canvas CSV exports
 */
@Document(collection = "classes")
public class Class {
    @Id
    private String id;

    @NotBlank(message = "Course code is required")
    @Indexed
    private String courseCode;

    @NotBlank(message = "Course name is required")
    private String courseName;

    private String section;

    @NotBlank(message = "Term is required")
    private String term;

    @NotNull(message = "Instructor ID is required")
    @Indexed
    private String instructorId;  // References User._id

    // Student IDs from Canvas CSV roster
    private List<String> roster = new ArrayList<>();

    // TA user IDs (upgraded from students)
    private List<String> taIds = new ArrayList<>();

    @NotNull
    private Instant createdAt;

    private Instant updatedAt;

    private Boolean archived = false;

    // Canvas LMS metadata
    private CanvasMetadata canvasMetadata;

    // Constructors
    public Class() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Class(String courseCode, String courseName, String term, String instructorId) {
        this();
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.term = term;
        this.instructorId = instructorId;
    }

    // Helper methods
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }

    public void addStudentToRoster(String studentId) {
        if (!this.roster.contains(studentId)) {
            this.roster.add(studentId);
            updateTimestamp();
        }
    }

    public void removeStudentFromRoster(String studentId) {
        this.roster.remove(studentId);
        updateTimestamp();
    }

    public void addTA(String taId) {
        if (!this.taIds.contains(taId)) {
            this.taIds.add(taId);
            updateTimestamp();
        }
    }

    public void removeTA(String taId) {
        this.taIds.remove(taId);
        updateTimestamp();
    }

    public boolean isStudentInRoster(String studentId) {
        return this.roster.contains(studentId);
    }

    public boolean isTA(String userId) {
        return this.taIds.contains(userId);
    }

    public boolean isInstructor(String userId) {
        return this.instructorId.equals(userId);
    }

    public boolean isStaff(String userId) {
        return isInstructor(userId) || isTA(userId);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(String instructorId) {
        this.instructorId = instructorId;
    }

    public List<String> getRoster() {
        return roster;
    }

    public void setRoster(List<String> roster) {
        this.roster = roster;
    }

    public List<String> getTaIds() {
        return taIds;
    }

    public void setTaIds(List<String> taIds) {
        this.taIds = taIds;
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

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public CanvasMetadata getCanvasMetadata() {
        return canvasMetadata;
    }

    public void setCanvasMetadata(CanvasMetadata canvasMetadata) {
        this.canvasMetadata = canvasMetadata;
    }
}
