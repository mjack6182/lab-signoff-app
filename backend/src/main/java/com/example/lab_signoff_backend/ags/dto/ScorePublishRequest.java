package com.example.lab_signoff_backend.ags.dto;

import com.example.lab_signoff_backend.ags.ActivityProgress;
import com.example.lab_signoff_backend.ags.GradingProgress;

import java.util.List;

public class ScorePublishRequest {
    public String courseId;
    public String lineItemId;         // optional
    public String groupId;            // optional
    public List<String> userIds;      // optional

    public String checkpointId;       // lab or column id

    // NEW: let server compute score if checkpoints present
    public List<CheckpointDto> checkpoints; // optional; if present, server computes score

    // Back-compat: if checkpoints == null/empty, use provided scores (validated)
    public Double scoreGiven;         // optional if checkpoints present
    public Double scoreMaximum;       // optional if checkpoints present

    public ActivityProgress activityProgress;  // optional if checkpoints present
    public GradingProgress  gradingProgress;   // optional if checkpoints present

    public Double lateMultiplier;     // optional; 0..1; default 1.0 when computing
    public String comment;            // optional; will be redacted by mock
    public String timestamp;          // optional ISO-8601

    public ScorePublishRequest() {}
}
