package com.example.lab_signoff_backend.csv;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StudentGrade {
    private String student;          // Canvas "Student" column (name)
    private String studentId;        // Canvas "ID" column
    private String labTitle;         // e.g., "Laboratory for Module 01 (9601577)"
    private BigDecimal score;        // student’s numeric score for that lab (nullable)
    private BigDecimal pointsPossible; // from the "Points Possible" row for that lab
    private LocalDateTime submittedAt; // timestamp of submission (nullable)


    public String getStudentId() {
        return studentId;
    }
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    public String getName() {
        return student;
    }
    public void setName(String student) {
        this.student = student;
    }
    public String getLab() {
        return labTitle;
    }
    public void setLab(String labTitle) {
        this.labTitle = labTitle;
    }
    public BigDecimal getScore() {
        return score;
    }
    public void setScore(BigDecimal score) {
        this.score = score;
    }
    public BigDecimal getMaxScore() {
        return pointsPossible;
    }
    public void setMaxScore(BigDecimal pointsPossible) {
        this.pointsPossible = pointsPossible;
    }
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}