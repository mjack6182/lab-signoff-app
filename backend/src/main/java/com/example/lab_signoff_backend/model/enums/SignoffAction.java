package com.example.lab_signoff_backend.model.enums;

/**
 * Action types for SignoffEvent documents
 */
public enum SignoffAction {
    PASS,       // Checkpoint passed
    RETURN,     // Checkpoint needs revision
    COMPLETE    // Final lab completion
}
