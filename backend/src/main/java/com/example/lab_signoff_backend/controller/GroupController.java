package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.CheckpointUpdate;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.repository.GroupRepository;
import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);

    private final LabWebSocketController wsController;
    private final GroupRepository groupRepository;

    @Autowired
    public GroupController(LabWebSocketController wsController, GroupRepository groupRepository) {
        this.wsController = wsController;
        this.groupRepository = groupRepository;
    }

    @GetMapping
    public ResponseEntity<List<Group>> getAllGroups() {
        return ResponseEntity.ok(groupRepository.findAll());
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<Group> getGroupById(@PathVariable String groupId) {
        return groupRepository.findById(groupId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{groupId}/checkpoints/{checkpointNumber}/toggle")
    public ResponseEntity<String> toggleCheckpoint(
            @PathVariable String groupId,
            @PathVariable int checkpointNumber,
            @RequestBody Map<String, Object> body
    ) {
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Group group = groupOpt.get();
        List<CheckpointProgress> checkpoints = group.getCheckpointProgress();
        if (checkpoints == null || checkpoints.isEmpty()) {
            return ResponseEntity.badRequest().body("No checkpoints found for this group");
        }

        boolean completed = (boolean) body.getOrDefault("completed", false);
        String performedBy = (String) body.getOrDefault("performedBy", "system");
        String notes = (String) body.getOrDefault("notes", null);

        CheckpointProgress target = checkpoints.stream()
                .filter(cp -> cp.getCheckpointNumber() == checkpointNumber)
                .findFirst()
                .orElse(null);

        if (target == null) {
            return ResponseEntity.badRequest().body("Checkpoint not found");
        }

        if (completed) {
            target.setStatus(SignoffAction.PASS);
            target.setSignedOffBy(performedBy);
            target.setSignedOffByName(performedBy);
            target.setTimestamp(Instant.now());
            target.setNotes(notes);
        } else {
            target.setStatus(SignoffAction.RETURN);
            target.setSignedOffBy(null);
            target.setSignedOffByName(null);
            target.setTimestamp(Instant.now());
            target.setNotes(notes);
        }

        groupRepository.save(group);

        CheckpointUpdate update = new CheckpointUpdate(
                group.getLabId(),
                groupId,
                checkpointNumber,
                completed ? "PASS" : "RETURN"
        );
        update.setSignedOffByName(performedBy);
        update.setNotes(notes);
        update.setTimestamp(Instant.now());

        wsController.broadcastCheckpointUpdate(group.getLabId(), update);

        return ResponseEntity.ok("Checkpoint " + checkpointNumber + " updated for group " + groupId);
    }

    @PostMapping("/{groupId}/pass")
    public ResponseEntity<String> passNextCheckpoint(@PathVariable String groupId) {
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) return ResponseEntity.notFound().build();

        Group group = groupOpt.get();
        List<CheckpointProgress> checkpoints = group.getCheckpointProgress();
        if (checkpoints == null || checkpoints.isEmpty()) return ResponseEntity.badRequest().body("No checkpoints found for this group");

        CheckpointProgress next = checkpoints.stream()
                .filter(cp -> cp.getStatus() == null || cp.getStatus() == SignoffAction.RETURN)
                .findFirst()
                .orElse(null);

        if (next == null) return ResponseEntity.badRequest().body("All checkpoints already passed");

        next.setStatus(SignoffAction.PASS);
        next.setSignedOffBy("system");
        next.setSignedOffByName("Auto");
        next.setTimestamp(Instant.now());

        groupRepository.save(group);

        CheckpointUpdate update = new CheckpointUpdate(
                group.getLabId(),
                groupId,
                next.getCheckpointNumber(),
                "PASS"
        );
        update.setSignedOffByName("Auto");
        update.setTimestamp(Instant.now());
        wsController.broadcastCheckpointUpdate(group.getLabId(), update);

        return ResponseEntity.ok("Checkpoint " + next.getCheckpointNumber() + " passed for group " + groupId);
    }
}