package com.example.lab_signoff_backend.model.embedded;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Embedded document representing a checkpoint definition in a lab
 */
public class CheckpointDefinition {
    @NotNull(message = "Checkpoint number is required")
    @Min(value = 1, message = "Checkpoint number must be at least 1")
    private Integer number;

    @NotBlank(message = "Checkpoint name is required")
    private String name;

    private String description;

    @NotNull(message = "Points are required")
    @Min(value = 1, message = "Points must be at least 1")
    private Integer points;

    private Boolean required = true;

    // Constructors
    public CheckpointDefinition() {
    }

    public CheckpointDefinition(Integer number, String name, String description, Integer points) {
        this.number = number;
        this.name = name;
        this.description = description;
        this.points = points;
        this.required = true;
    }

    // Getters and Setters
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
