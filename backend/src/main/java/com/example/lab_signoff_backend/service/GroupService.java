package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.repository.GroupRepository;
import com.example.lab_signoff_backend.repository.LabRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class GroupService {

    private final GroupRepository repo;
    private final LabRepository labRepo;

    public GroupService(GroupRepository repo, LabRepository labRepo) {
        this.repo = repo;
        this.labRepo = labRepo;
    }

    public List<Group> getGroupsByLabId(String labId) {
        List<Group> groups = repo.findByLabId(labId);
        for (Group g : groups) {
            autoInitCheckpoints(g);
        }
        return groups;
    }

    public List<Group> getAll() {
        List<Group> groups = repo.findAll();
        for (Group g : groups) {
            autoInitCheckpoints(g);
        }
        return groups;
    }

    public Group upsert(Group group) {
        autoInitCheckpoints(group);
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

        Optional<Group> maybeGroup = repo.findByGroupId(groupIdOrId);
        if (maybeGroup.isEmpty()) {
            maybeGroup = repo.findById(groupIdOrId);
        }

        if (maybeGroup.isEmpty()) {
            throw new RuntimeException("Group not found: " + groupIdOrId);
        }

        Group group = maybeGroup.get();

        autoInitCheckpoints(group);

        List<CheckpointProgress> progressList = group.getCheckpointProgress();

        CheckpointProgress target = progressList.stream()
                .filter(cp -> Objects.equals(cp.getCheckpointNumber(), checkpointNumber))
                .findFirst()
                .orElse(null);

        if (target == null) {
            target = new CheckpointProgress();
            target.setCheckpointNumber(checkpointNumber);
            progressList.add(target);
        }

        SignoffAction action = SignoffAction.valueOf(statusString);
        target.setStatus(action);
        target.setSignedOffBy(signedOffBy);
        target.setSignedOffByName(signedOffByName);
        target.setTimestamp(Instant.now());
        target.setNotes(notes);
        target.setPointsAwarded(pointsAwarded);

        group.updateTimestamp();
        Group saved = repo.save(group);

        return saved.getCheckpointProgress().stream()
                .filter(cp -> Objects.equals(cp.getCheckpointNumber(), checkpointNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Failed to persist checkpoint progress"));
    }

    private void autoInitCheckpoints(Group group) {
        if (group.getCheckpointProgress() != null && !group.getCheckpointProgress().isEmpty()) {
            return;
        }

        Optional<Lab> labOpt = labRepo.findById(group.getLabId());
        if (labOpt.isEmpty()) {
            throw new RuntimeException("Lab not found for group " + group.getId());
        }

        Lab lab = labOpt.get();
        int totalCheckpoints = lab.getPoints();

        List<CheckpointProgress> list = new ArrayList<>();

        for (int i = 1; i <= totalCheckpoints; i++) {
            CheckpointProgress cp = new CheckpointProgress();
            cp.setCheckpointNumber(i);
            cp.setStatus(null);
            cp.setSignedOffBy(null);
            cp.setSignedOffByName(null);
            cp.setTimestamp(null);
            cp.setNotes("");
            cp.setPointsAwarded(null);
            list.add(cp);
        }

        group.setCheckpointProgress(list);
        repo.save(group);
    }
}