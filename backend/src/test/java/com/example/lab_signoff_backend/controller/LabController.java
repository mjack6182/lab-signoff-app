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

    @PostMapping("/labs")
    public Lab createOrUpdateLab(@RequestBody Lab lab) {
        return labService.upsert(lab);
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
                                       @RequestParam(required = false) String performedBy,
                                       @RequestParam(required = false) String notes) {

        if (labId == null || labId.trim().isEmpty()) return ResponseEntity.badRequest().body("Lab ID cannot be empty");
        if (groupId == null || groupId.trim().isEmpty()) return ResponseEntity.badRequest().body("Group ID cannot be empty");

        if (!labService.labExists(labId)) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Lab with ID " + labId + " not found");

        Optional<Group> groupOpt = groupService.getAll().stream()
                .filter(g -> g.getGroupId().equals(groupId) && g.getLabId().equals(labId))
                .findFirst();

        if (groupOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Group with ID " + groupId + " not found in lab " + labId);

        Group group = groupOpt.get();
        group.setStatus(GroupStatus.SIGNED_OFF);
        groupService.upsert(group);

        String performer = (performedBy != null && !performedBy.trim().isEmpty()) ? performedBy : "system";
        SignoffEvent event = signoffEventService.createEvent(
                labId,
                groupId,
                "PASS",
                performer,
                notes,
                null
        );

        wsController.broadcastGroupPassed(labId, groupId);

        return ResponseEntity.ok(new PassReturnResponse(group, event, "Group successfully marked as passed and broadcasted"));
    }

    @PostMapping("/labs/{labId}/groups/{groupId}/return")
    public ResponseEntity<?> returnGroup(@PathVariable String labId,
                                         @PathVariable String groupId,
                                         @RequestParam(required = false) String performedBy,
                                         @RequestParam(required = false) String notes) {

        if (labId == null || labId.trim().isEmpty()) return ResponseEntity.badRequest().body("Lab ID cannot be empty");
        if (groupId == null || groupId.trim().isEmpty()) return ResponseEntity.badRequest().body("Group ID cannot be empty");

        if (!labService.labExists(labId)) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Lab with ID " + labId + " not found");

        Optional<Group> groupOpt = groupService.getAll().stream()
                .filter(g -> g.getGroupId().equals(groupId) && g.getLabId().equals(labId))
                .findFirst();

        if (groupOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Group with ID " + groupId + " not found in lab " + labId);

        Group group = groupOpt.get();
        group.setStatus(GroupStatus.IN_PROGRESS);
        groupService.upsert(group);

        String performer = (performedBy != null && !performedBy.trim().isEmpty()) ? performedBy : "system";
        SignoffEvent event = signoffEventService.createEvent(
                labId,
                groupId,
                "RETURN",
                performer,
                notes,
                null
        );

        return ResponseEntity.ok(new PassReturnResponse(group, event, "Group successfully marked as returned"));
    }

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
}