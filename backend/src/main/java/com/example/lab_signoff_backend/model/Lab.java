
package com.example.lab_signoff_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "labs")
public class Lab {
    @Id
    private String id;

    private int activeCheckpointNo;
    private String courseId;
    private String createdAt;
    private String createdBy;
    private String externalUrl;

    private String title;
    private int points;

    public Lab() {
    }

    public Lab(String id, String title, int points) {
        this.id = id;
        this.title = title;
        this.points = points;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getPoints() {
        return points;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getActiveCheckpointNo() {
        return activeCheckpointNo;
    }

    public void setActiveCheckpointNo(int activeCheckpointNo) {
        this.activeCheckpointNo = activeCheckpointNo;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

}
