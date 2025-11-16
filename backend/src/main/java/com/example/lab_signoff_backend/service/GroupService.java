package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.repository.GroupRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Handles Group operations and checkpoint progress updates.
 */
@Service
public class GroupService {

    private final GroupRepository repo;

    public GroupService(GroupRepository repo) {
        this.repo = repo;
    }

    public List<Group> getGroupsByLabId(String labId) {
        return repo.findByLabId(labId);
    }

    public List<Group> getAll() {
        return repo.findAll();
    }

    public Group upsert(Group group) {
        return repo.save(group);
    }

    public CheckpointProgress updateCheckpointProgress(
            String groupIdOrId,
            Integer checkpointNumber,
            String statusString,
            String signedOffBy,
            String signedOffByName,
            String notes,
            Integer pointsAwarded) {

        // Try finding the group either by custom groupId or Mongo _id
        Optional<Group> maybeGroup = repo.findByGroupId(groupIdOrId);
        if (maybeGroup.isEmpty()) {
            maybeGroup = repo.findById(groupIdOrId);
        }

        if (maybeGroup.isEmpty()) {
            throw new RuntimeException("Group not found: " + groupIdOrId);
        }

        Group group = maybeGroup.get();

        // Initialize checkpoint list if null
        List<CheckpointProgress> progressList = group.getCheckpointProgress();
        if (progressList == null) {
            progressList = new ArrayList<>();
            group.setCheckpointProgress(progressList);
        }

        // Find or create the specific checkpoint
        CheckpointProgress target = null;
        for (CheckpointProgress cp : progressList) {
            if (Objects.equals(cp.getCheckpointNumber(), checkpointNumber)) {
                target = cp;
                break;
            }
        }

        if (target == null) {
            target = new CheckpointProgress();
            target.setCheckpointNumber(checkpointNumber);
            progressList.add(target); // Now safe — list is guaranteed non-null
        }

        // Update checkpoint info
        SignoffAction action = SignoffAction.valueOf(statusString); // e.g., "PASS" or "RETURN"
        target.setStatus(action);
        target.setSignedOffBy(signedOffBy);
        target.setSignedOffByName(signedOffByName);
        target.setTimestamp(Instant.now());
        target.setNotes(notes);
        target.setPointsAwarded(pointsAwarded);

        // Update group timestamps and persist
        group.updateTimestamp();
        Group saved = repo.save(group);

        // Return updated checkpoint
        return saved.getCheckpointProgress().stream()
                .filter(cp -> Objects.equals(cp.getCheckpointNumber(), checkpointNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Failed to persist checkpoint progress"));
    }
}