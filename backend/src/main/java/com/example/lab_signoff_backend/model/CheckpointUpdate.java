package com.example.lab_signoff_backend.model;

public class CheckpointUpdate {
    private String groupId;
    private int checkpointNumber;
    private String status; // "PASS" or "RETURN"

    public CheckpointUpdate() {}

    public CheckpointUpdate(String groupId, int checkpointNumber, String status) {
        this.groupId = groupId;
        this.checkpointNumber = checkpointNumber;
        this.status = status;
    }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public int getCheckpointNumber() { return checkpointNumber; }
    public void setCheckpointNumber(int checkpointNumber) { this.checkpointNumber = checkpointNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
