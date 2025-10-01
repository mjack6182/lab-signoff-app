package com.example.lab_signoff_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Lab model class representing a laboratory assignment or exercise.
 * This class is mapped to the "labs" collection in MongoDB.
 * 
 * A Lab contains information about course assignments including metadata
 * such as creation details, course association, and external resources.
 */
@Document(collection = "labs")
public class Lab {
    /**
     * Unique identifier for the lab assignment
     */
    @Id
    private String id;
    private String courseId;
    private String lineItemId;

    /**
     * Default constructor for Lab
     * Required for Spring Data MongoDB serialization/deserialization
     */
    public Lab() {
    }

    /**
     * Constructor for creating a Lab with basic information
     * 
     * @param id         Unique identifier for the lab
     * @param courseId   Identifier of the course this lab belongs to
     * @param lineItemId Identifier of the line item for this lab
     */
    public Lab(String id, String courseId, String lineItemId) {
        this.id = id;
        this.courseId = courseId;
        this.lineItemId = lineItemId;
    }

    public String getId() {
        return id;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getLineItemId() {
        return lineItemId;
    }

}
