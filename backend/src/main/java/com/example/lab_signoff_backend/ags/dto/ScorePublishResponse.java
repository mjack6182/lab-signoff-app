package com.example.lab_signoff_backend.ags.dto;

import com.example.lab_signoff_backend.ags.ActivityProgress;
import com.example.lab_signoff_backend.ags.GradingProgress;

import java.util.List;

public class ScorePublishResponse {
    public String status = "ok";
    public boolean mock = true;

    public String courseId;
    public String lineItemId;
    public List<String> resultIds;
    public int syncedCount;

    public String checkpointId;
    public GradingProgress gradingProgress;
    public ActivityProgress activityProgress;
    public String timestamp;

    public String mockSyncId;  // simple server-generated id

    public ScorePublishResponse() {}
}
