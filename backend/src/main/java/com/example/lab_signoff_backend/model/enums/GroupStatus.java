package com.example.lab_signoff_backend.model.enums;

/**
 * Status values for Group documents
 */
public enum GroupStatus {
    FORMING,         // Group is being formed, students joining
    IN_PROGRESS,     // Group is actively working on checkpoints
    COMPLETED,       // All checkpoints completed
    SIGNED_OFF       // Final sign-off by instructor
}
