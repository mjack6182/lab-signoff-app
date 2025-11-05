package com.example.lab_signoff_backend.model.embedded;

/**
 * Embedded document for Canvas LMS metadata
 */
public class CanvasMetadata {
    private String lineItemId;
    private String courseId;
    private String contextId;
    private String deploymentId;
    private String resourceLinkId;

    // Constructors
    public CanvasMetadata() {
    }

    public CanvasMetadata(String lineItemId, String courseId) {
        this.lineItemId = lineItemId;
        this.courseId = courseId;
    }

    // Getters and Setters
    public String getLineItemId() {
        return lineItemId;
    }

    public void setLineItemId(String lineItemId) {
        this.lineItemId = lineItemId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getResourceLinkId() {
        return resourceLinkId;
    }

    public void setResourceLinkId(String resourceLinkId) {
        this.resourceLinkId = resourceLinkId;
    }
}
