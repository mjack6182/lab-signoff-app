package com.example.lab_signoff_backend.model.enums;

/**
 * Status values for Lab documents
 */
public enum LabStatus {
    DRAFT,        // Lab created but not yet active
    ACTIVE,       // Lab is active and students can join
    CLOSED,       // Lab has ended, no more sign-offs
    ARCHIVED      // Lab is archived for historical records
}
