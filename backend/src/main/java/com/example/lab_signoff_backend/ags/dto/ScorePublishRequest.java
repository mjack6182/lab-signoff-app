package com.example.lab_signoff_backend.ags.dto;

import com.example.lab_signoff_backend.ags.ActivityProgress;
import com.example.lab_signoff_backend.ags.GradingProgress;

import java.util.List;

public class ScorePublishRequest {
    public String status = "ok";
    public boolean mock = true;

    public String courseId;
    public String lineItemId;
    public List<String> resultIds;
    public int syncedCount;

    public String checkpointId;

    // NEW: always return computed/final numbers
    public Double scoreGiven;
    public Double scoreMaximum;

    public GradingProgress gradingProgress;
    public ActivityProgress activityProgress;
    public String timestamp;

    public String mockSyncId;

    public ScorePublishRequest() {}
}  // simple server-generated id
