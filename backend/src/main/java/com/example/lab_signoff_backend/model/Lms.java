package com.example.lab_signoff_backend.model;

public class Lms {
    private String lineItemId;
    private String courseId;

    public Lms() {
    }

    public Lms(String lineItemId, String courseId) {
        this.lineItemId = lineItemId;
        this.courseId = courseId;
    }

    public String getLineItemId() {
        return lineItemId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setLineItemId(String lineItemId) {
        this.lineItemId = lineItemId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
}
