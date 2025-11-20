package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.SignoffEvent;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import com.example.lab_signoff_backend.service.GroupService;
import com.example.lab_signoff_backend.service.LabService;
import com.example.lab_signoff_backend.service.SignoffEventService;
import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/lti")
public class LabController {

    private final LabService labService;
    private final GroupService groupService;
    private final SignoffEventService signoffEventService;
    private final LabWebSocketController wsController;

    @Autowired
    public LabController(LabService labService,
                         GroupService groupService,
                         SignoffEventService signoffEventService,
                         LabWebSocketController wsController) {
        this.labService = labService;
        this.groupService = groupService;
        this.signoffEventService = signoffEventService;
        this.wsController = wsController;
    }

    @GetMapping("/labs")
    public List<Lab> getLabs() {
        return labService.getAll();
    }

    @GetMapping("/labs/{id}/groups")
    public ResponseEntity<?> getGroupsByLabId(@PathVariable String id) {
        if (id == null || id.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Lab ID cannot be empty");
        }

        if (!labService.labExists(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Lab with ID " + id + " not found");
        }

        List<Group> groups = groupService.getGroupsByLabId(id);
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/labs/{labId}/groups/{groupId}/pass")
    public ResponseEntity<?> passGroup(@PathVariable String labId,
                                       @PathVariable String groupId,
                                       @RequestBody(required = false) Map<String, Object> payload) {

        String performedBy = payload != null && payload.get("performedBy") != null
                ? payload.get("performedBy").toString()
                : "system";

        String notes = payload != null && payload.get("notes") != null
                ? payload.get("notes").toString()
                : null;

        if (labId == null || labId.trim().isEmpty()) return ResponseEntity.badRequest().body("Lab ID cannot be empty");
        if (groupId == null || groupId.trim().isEmpty()) return ResponseEntity.badRequest().body("Group ID cannot be empty");

        if (!labService.labExists(labId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Lab with ID " + labId + " not found");

        Optional<Group> groupOpt = groupService.getAll().stream()
                .filter(g -> g.getGroupId().equals(groupId) && g.getLabId().equals(labId))
                .findFirst();

        if (groupOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Group with ID " + groupId + " not found in lab " + labId);

        Group group = groupOpt.get();
        group.setStatus(GroupStatus.SIGNED_OFF);
        groupService.upsert(group);

        SignoffEvent event = signoffEventService.createEvent(
                labId,
                groupId,
                "PASS",
                performedBy,
                notes,
                null
        );

        wsController.broadcastGroupPassed(labId, groupId);

        return ResponseEntity.ok(new PassReturnResponse(group, event, "Group successfully marked as passed and broadcasted"));
    }

    @PostMapping("/labs/{labId}/groups/{groupId}/return")
    public ResponseEntity<?> returnGroup(@PathVariable String labId,
                                         @PathVariable String groupId,
                                         @RequestBody(required = false) Map<String, Object> payload) {

        String performedBy = payload != null && payload.get("performedBy") != null
                ? payload.get("performedBy").toString()
                : "system";

        String notes = payload != null && payload.get("notes") != null
                ? payload.get("notes").toString()
                : null;

        if (labId == null || labId.trim().isEmpty()) return ResponseEntity.badRequest().body("Lab ID cannot be empty");
        if (groupId == null || groupId.trim().isEmpty()) return ResponseEntity.badRequest().body("Group ID cannot be empty");

        if (!labService.labExists(labId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Lab with ID " + labId + " not found");

        Optional<Group> groupOpt = groupService.getAll().stream()
                .filter(g -> g.getGroupId().equals(groupId) && g.getLabId().equals(labId))
                .findFirst();

        if (groupOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Group with ID " + groupId + " not found in lab " + labId);

        Group group = groupOpt.get();
        group.setStatus(GroupStatus.IN_PROGRESS);
        groupService.upsert(group);

        SignoffEvent event = signoffEventService.createEvent(
                labId,
                groupId,
                "RETURN",
                performedBy,
                notes,
                null
        );

        return ResponseEntity.ok(new PassReturnResponse(group, event, "Group successfully marked as returned"));
    }

    /**
     * Randomize groups for a lab based on enrolled students.
     * Deletes existing groups and creates new randomized groups.
     */
    @PostMapping("/labs/{labId}/randomize-groups")
    public ResponseEntity<?> randomizeGroups(@PathVariable String labId) {
        if (labId == null || labId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Lab ID cannot be empty");
        }

        try {
            if (!labService.labExists(labId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Lab with ID " + labId + " not found");
            }

            List<Group> newGroups = groupService.randomizeGroups(labId);

            // Broadcast WebSocket update to notify all clients
            wsController.broadcastGroupsRandomized(labId);

            return ResponseEntity.ok()
                    .body(new RandomizeGroupsResponse(
                            newGroups,
                            newGroups.size(),
                            "Groups successfully randomized"
                    ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error randomizing groups: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error randomizing groups: " + e.getMessage());
        }
    }

    /**
     * Bulk update groups for a lab.
     * Replaces all existing groups with the provided groups.
     */
    @PutMapping("/labs/{labId}/groups")
    public ResponseEntity<?> updateGroups(
            @PathVariable String labId,
            @RequestBody List<Group> groups) {

        if (labId == null || labId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Lab ID cannot be empty");
        }

        if (groups == null) {
            return ResponseEntity.badRequest().body("Groups list cannot be null");
        }

        try {
            if (!labService.labExists(labId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Lab with ID " + labId + " not found");
            }

            List<Group> updatedGroups = groupService.bulkUpdateGroups(labId, groups);

            // Broadcast WebSocket update to notify all clients
            wsController.broadcastGroupsRandomized(labId);

            return ResponseEntity.ok()
                    .body(new UpdateGroupsResponse(
                            updatedGroups,
                            updatedGroups.size(),
                            "Groups successfully updated"
                    ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error updating groups: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating groups: " + e.getMessage());
        }
    }

    /** Response class for pass/return operations. */
    private static class PassReturnResponse {
        private final Group group;
        private final SignoffEvent event;
        private final String message;

        public PassReturnResponse(Group group, SignoffEvent event, String message) {
            this.group = group;
            this.event = event;
            this.message = message;
        }

        public Group getGroup() { return group; }
        public SignoffEvent getEvent() { return event; }
        public String getMessage() { return message; }
    }

    /** Response class for randomize groups operation. */
    private static class RandomizeGroupsResponse {
        private final List<Group> groups;
        private final int groupCount;
        private final String message;

        public RandomizeGroupsResponse(List<Group> groups, int groupCount, String message) {
            this.groups = groups;
            this.groupCount = groupCount;
            this.message = message;
        }

        public List<Group> getGroups() { return groups; }
        public int getGroupCount() { return groupCount; }
        public String getMessage() { return message; }
    }

    /** Response class for update groups operation. */
    private static class UpdateGroupsResponse {
        private final List<Group> groups;
        private final int groupCount;
        private final String message;

        public UpdateGroupsResponse(List<Group> groups, int groupCount, String message) {
            this.groups = groups;
            this.groupCount = groupCount;
            this.message = message;
        }

        public List<Group> getGroups() { return groups; }
        public int getGroupCount() { return groupCount; }
        public String getMessage() { return message; }
    }
}