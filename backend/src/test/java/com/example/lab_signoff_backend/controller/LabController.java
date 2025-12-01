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

    // Services that handle DB operations + websocket broadcasts
    private final LabService labService;
    private final GroupService groupService;
    private final SignoffEventService signoffEventService;
    private final LabWebSocketController wsController;

    @Autowired
    public LabController(
            LabService labService,
            GroupService groupService,
            SignoffEventService signoffEventService,
            LabWebSocketController wsController
    ) {
        this.labService = labService;
        this.groupService = groupService;
        this.signoffEventService = signoffEventService;
        this.wsController = wsController;
    }

    // --------------------------------------------------------
    // GET: return all labs in the system
    // --------------------------------------------------------
    @GetMapping("/labs")
    public List<Lab> getLabs() {
        return labService.getAll();
    }

    // --------------------------------------------------------
    // POST: create or update a Lab (upsert)
    // --------------------------------------------------------
    @PostMapping("/labs")
    public Lab createOrUpdateLab(@RequestBody Lab lab) {
        return labService.upsert(lab);
    }

    // --------------------------------------------------------
    // GET: return all groups belonging to a given lab
    // --------------------------------------------------------
    @GetMapping("/labs/{id}/groups")
    public ResponseEntity<?> getGroupsByLabId(@PathVariable String id) {

        // Basic validation
        if (id == null || id.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Lab ID cannot be empty");
        }

        // Ensure lab exists
        if (!labService.labExists(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Lab with ID " + id + " not found");
        }

        // Fetch and return groups
        List<Group> groups = groupService.getGroupsByLabId(id);
        return ResponseEntity.ok(groups);
    }

    // --------------------------------------------------------
    // POST: Mark entire group as passed (signed off)
    // Also broadcasts via WebSocket so UI updates instantly
    // --------------------------------------------------------
    @PostMapping("/labs/{labId}/groups/{groupId}/pass")
    public ResponseEntity<?> passGroup(
            @PathVariable String labId,
            @PathVariable String groupId,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) String notes
    ) {

        // Validate required params
        if (labId == null || labId.trim().isEmpty())
            return ResponseEntity.badRequest().body("Lab ID cannot be empty");

        if (groupId == null || groupId.trim().isEmpty())
            return ResponseEntity.badRequest().body("Group ID cannot be empty");

        // Confirm lab exists
        if (!labService.labExists(labId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Lab with ID " + labId + " not found");

        // Find group by lab + groupId
        Optional<Group> groupOpt = groupService.getAll().stream()
                .filter(g -> g.getGroupId().equals(groupId) && g.getLabId().equals(labId))
                .findFirst();

        if (groupOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Group with ID " + groupId + " not found in lab " + labId);

        // Mark group as SIGNED_OFF
        Group group = groupOpt.get();
        group.setStatus(GroupStatus.SIGNED_OFF);
        groupService.upsert(group); // save update

        // Choose performer name or default
        String performer = (performedBy != null && !performedBy.trim().isEmpty())
                ? performedBy
                : "system";

        // Create historical signoff event for backend records
        SignoffEvent event = signoffEventService.createEvent(
                labId,
                groupId,
                "PASS",
                performer,
                notes,
                null
        );

        // Notify all connected clients via WebSocket
        wsController.broadcastGroupPassed(labId, groupId);

        // Return structured response
        return ResponseEntity.ok(
                new PassReturnResponse(group, event, "Group successfully marked as passed and broadcasted")
        );
    }

    // --------------------------------------------------------
    // POST: Return (undo) a group's completed state
    // Does NOT broadcast on WebSocket here, but could if needed
    // --------------------------------------------------------
    @PostMapping("/labs/{labId}/groups/{groupId}/return")
    public ResponseEntity<?> returnGroup(
            @PathVariable String labId,
            @PathVariable String groupId,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) String notes
    ) {

        // Basic validation
        if (labId == null || labId.trim().isEmpty())
            return ResponseEntity.badRequest().body("Lab ID cannot be empty");

        if (groupId == null || groupId.trim().isEmpty())
            return ResponseEntity.badRequest().body("Group ID cannot be empty");

        // Lab existence check
        if (!labService.labExists(labId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Lab with ID " + labId + " not found");

        // Find group inside this lab
        Optional<Group> groupOpt = groupService.getAll().stream()
                .filter(g -> g.getGroupId().equals(groupId) && g.getLabId().equals(labId))
                .findFirst();

        if (groupOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Group with ID " + groupId + " not found in lab " + labId);

        // Update status back to IN_PROGRESS
        Group group = groupOpt.get();
        group.setStatus(GroupStatus.IN_PROGRESS);
        groupService.upsert(group);

        // Determine performer
        String performer = (performedBy != null && !performedBy.trim().isEmpty())
                ? performedBy
                : "system";

        // Log return event in DB
        SignoffEvent event = signoffEventService.createEvent(
                labId,
                groupId,
                "RETURN",
                performer,
                notes,
                null
        );

        return ResponseEntity.ok(
                new PassReturnResponse(group, event, "Group successfully marked as returned")
        );
    }

    /**
     * Small helper class to return group + event + message together.
     * Used for both PASS and RETURN responses.
     */
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
