package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.SignoffEvent;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.CheckpointUpdate;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.repository.GroupRepository;
import com.example.lab_signoff_backend.service.SignoffEventService;
import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Controller for persisting and broadcasting checkpoint signoffs.
 */
@RestController
@RequestMapping("/labs/{labId}/groups")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")

public class LabGroupController {

    private static final Logger logger = LoggerFactory.getLogger(LabGroupController.class);

    private final GroupRepository groupRepository;
    private final SignoffEventService signoffEventService;
    private final LabWebSocketController wsController;

    @Autowired
    public LabGroupController(GroupRepository groupRepository,
                              SignoffEventService signoffEventService,
                              LabWebSocketController wsController) {
        this.groupRepository = groupRepository;
        this.signoffEventService = signoffEventService;
        this.wsController = wsController;
    }

    @PostMapping("/{groupId}/pass")
    public ResponseEntity<?> passCheckpointPersisted(
            @PathVariable String labId,
            @PathVariable String groupId,
            @RequestBody Map<String, Object> body
    ) {
        try {
            Optional<Group> groupOpt = groupRepository.findByGroupId(groupId);
            if (groupOpt.isEmpty()) {
    groupOpt = groupRepository.findById(groupId);
}

            if (groupOpt.isEmpty()) {
            logger.warn("Group not found for ID: {}", groupId);
                         return ResponseEntity.status(404).body("Group not found");
            }

            Group group = groupOpt.get();

            Integer checkpointNumber = ((Number) body.getOrDefault("checkpointNumber", 1)).intValue();
            String notes = (String) body.getOrDefault("notes", "");
            String performedBy = (String) body.getOrDefault("performedBy", "system");

            List<CheckpointProgress> checkpoints = group.getCheckpointProgress();
            if (checkpoints == null || checkpoints.isEmpty()) {
                return ResponseEntity.badRequest().body("No checkpoints initialized for this group");
            }

            CheckpointProgress target = checkpoints.stream()
                    .filter(cp -> Objects.equals(cp.getCheckpointNumber(), checkpointNumber))
                    .findFirst()
                    .orElse(null);

            if (target == null) {
                logger.warn("Checkpoint {} not found in group {}", checkpointNumber, groupId);
                return ResponseEntity.badRequest().body("Checkpoint not found");
            }

            target.setStatus(SignoffAction.PASS);
            target.setSignedOffBy(performedBy);
            target.setSignedOffByName(performedBy);
            target.setTimestamp(Instant.now());
            target.setNotes(notes);

            groupRepository.save(group);

            SignoffEvent event = new SignoffEvent();
            event.setLabId(labId);
            event.setGroupId(groupId);
            event.setCheckpointNumber(checkpointNumber);
            event.setAction(SignoffAction.PASS);
            event.setPerformedBy(performedBy);
            event.setTimestamp(Instant.now());
            event.setNotes(notes);

            SignoffEvent savedEvent = signoffEventService.createEvent(event);

            CheckpointUpdate update = new CheckpointUpdate(labId, groupId, checkpointNumber, "PASS");
            update.setSignedOffByName(performedBy);
            update.setNotes(notes);
            update.setTimestamp(savedEvent.getTimestamp());

            wsController.broadcastCheckpointUpdate(labId, update);

            logger.info("✅ Persisted PASS signoff: lab={}, group={}, cp={}, eventId={}",
                    labId, groupId, checkpointNumber, savedEvent.getId());

            return ResponseEntity.ok(Map.of("status", "ok", "eventId", savedEvent.getId()));
        } catch (Exception ex) {
            logger.error("❌ Failed to persist PASS signoff for lab {} group {}: {}", labId, groupId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Internal server error: " + ex.getMessage());
        }
    }

    @PostMapping("/{groupId}/return")
    public ResponseEntity<?> returnCheckpointPersisted(
            @PathVariable String labId,
            @PathVariable String groupId,
            @RequestBody Map<String, Object> body
    ) {
        try {
            Optional<Group> groupOpt = groupRepository.findByGroupId(groupId);
if (groupOpt.isEmpty()) {
    groupOpt = groupRepository.findById(groupId);
}

if (groupOpt.isEmpty()) {
    return ResponseEntity.status(404).body("Group not found");
}

            Group group = groupOpt.get();

            Integer checkpointNumber = ((Number) body.getOrDefault("checkpointNumber", 1)).intValue();
            String notes = (String) body.getOrDefault("notes", "");
            String performedBy = (String) body.getOrDefault("performedBy", "system");

            List<CheckpointProgress> checkpoints = group.getCheckpointProgress();
            if (checkpoints == null || checkpoints.isEmpty()) {
                return ResponseEntity.badRequest().body("No checkpoints initialized for this group");
            }

            CheckpointProgress target = checkpoints.stream()
                    .filter(cp -> Objects.equals(cp.getCheckpointNumber(), checkpointNumber))
                    .findFirst()
                    .orElse(null);

            if (target == null) {
                return ResponseEntity.badRequest().body("Checkpoint not found");
            }

            target.setStatus(SignoffAction.RETURN);
            target.setSignedOffBy(null);
            target.setSignedOffByName(null);
            target.setTimestamp(Instant.now());
            target.setNotes(notes);

            groupRepository.save(group);

            SignoffEvent event = new SignoffEvent();
            event.setLabId(labId);
            event.setGroupId(groupId);
            event.setCheckpointNumber(checkpointNumber);
            event.setAction(SignoffAction.RETURN);
            event.setPerformedBy(performedBy);
            event.setTimestamp(Instant.now());
            event.setNotes(notes);

            SignoffEvent savedEvent = signoffEventService.createEvent(event);

            CheckpointUpdate update = new CheckpointUpdate(labId, groupId, checkpointNumber, "RETURN");
            update.setSignedOffByName(performedBy);
            update.setNotes(notes);
            update.setTimestamp(savedEvent.getTimestamp());

            wsController.broadcastCheckpointUpdate(labId, update);

            logger.info("✅ Persisted RETURN signoff: lab={}, group={}, cp={}, eventId={}",
                    labId, groupId, checkpointNumber, savedEvent.getId());

            return ResponseEntity.ok(Map.of("status", "ok", "eventId", savedEvent.getId()));
        } catch (Exception ex) {
            logger.error("❌ Failed to persist RETURN signoff for lab {} group {}: {}", labId, groupId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Internal server error: " + ex.getMessage());
        }
    }
}