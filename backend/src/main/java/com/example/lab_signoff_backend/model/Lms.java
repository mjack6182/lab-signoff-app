package com.example.lab_signoff_backend.model;

/**
 * LMS (Learning Management System) model class.
 *
 * This class represents LMS-specific information for a lab assignment,
 * including Canvas LTI integration details such as line items and course identifiers.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
public class Lms {
    /** LTI line item identifier for gradebook integration */
    private String lineItemId;

    /** Course identifier from the LMS */
    private String courseId;

    /**
     * Default constructor for Lms.
     */
    public Lms() {
    }

    /**
     * Constructor for creating an Lms with all fields.
     *
     * @param lineItemId The LTI line item identifier
     * @param courseId   The course identifier
     */
    public Lms(String lineItemId, String courseId) {
        this.lineItemId = lineItemId;
        this.courseId = courseId;
    }

    /**
     * Gets the line item identifier.
     *
     * @return The line item identifier
     */
    public String getLineItemId() {
        return lineItemId;
    }

    /**
     * Gets the course identifier.
     *
     * @return The course identifier
     */
    public String getCourseId() {
        return courseId;
    }

    /**
     * Sets the line item identifier.
     *
     * @param lineItemId The line item identifier to set
     */
    public void setLineItemId(String lineItemId) {
        this.lineItemId = lineItemId;
    }

    /**
     * Sets the course identifier.
     *
     * @param courseId The course identifier to set
     */
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
}
