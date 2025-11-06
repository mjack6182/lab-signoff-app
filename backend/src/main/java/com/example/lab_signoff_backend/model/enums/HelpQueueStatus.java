package com.example.lab_signoff_backend.model.enums;

/**
 * Status values for HelpQueueItem documents
 */
public enum HelpQueueStatus {
    WAITING,     // Request submitted, waiting for help
    CLAIMED,     // TA/Teacher has claimed the request
    RESOLVED,    // Help request resolved
    CANCELLED    // Request cancelled by student
}
