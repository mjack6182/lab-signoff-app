package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.repository.GroupRepository;
import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        List<Group> groups = groupRepository.findAll();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<Group> getGroupById(@PathVariable String groupId) {
        Optional<Group> group = groupRepository.findById(groupId);
        return group.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ Dynamic checkpoint pass
    @PostMapping("/{groupId}/pass")
    public ResponseEntity<String> passCheckpoint(@PathVariable String groupId,
                                                 @RequestBody Map<String, Object> body) {
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            logger.warn("Attempted to pass checkpoint for non-existent group: {}", groupId);
            return ResponseEntity.notFound().build();
        }

        // Extract checkpoint number from JSON
        int checkpointNumber = ((Number) body.getOrDefault("checkpointNumber", 1)).intValue();
        wsController.broadcastCheckpointUpdate(groupId, checkpointNumber, "PASS");

        logger.info("✅ Group {} passed checkpoint {}", groupId, checkpointNumber);
        return ResponseEntity.ok("Broadcasted PASS update for group " + groupId +
                                 " checkpoint " + checkpointNumber);
    }

    // ✅ Dynamic checkpoint return
    @PostMapping("/{groupId}/return")
    public ResponseEntity<String> returnCheckpoint(@PathVariable String groupId,
                                                   @RequestBody Map<String, Object> body) {
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            logger.warn("Attempted to return checkpoint for non-existent group: {}", groupId);
            return ResponseEntity.notFound().build();
        }

        int checkpointNumber = ((Number) body.getOrDefault("checkpointNumber", 1)).intValue();
        wsController.broadcastCheckpointUpdate(groupId, checkpointNumber, "RETURN");

        logger.info("♻️ Group {} returned checkpoint {}", groupId, checkpointNumber);
        return ResponseEntity.ok("Broadcasted RETURN update for group " + groupId +
                                 " checkpoint " + checkpointNumber);
    }
}