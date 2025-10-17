package com.example.lab_signoff_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.SignoffEvent;
import com.example.lab_signoff_backend.service.GroupService;
import com.example.lab_signoff_backend.service.LabService;
import com.example.lab_signoff_backend.service.SignoffEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for lab and group management operations.
 *
 * Provides endpoints for retrieving labs, creating/updating labs,
 * and fetching groups associated with specific labs.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@RestController
@RequestMapping("/lti")
public class LabController {

    private final LabService labService;
    private final GroupService groupService;
    private final SignoffEventService signoffEventService;

    /**
     * Constructor for LabController.
     *
     * @param labService           Service for lab operations
     * @param groupService         Service for group operations
     * @param signoffEventService  Service for signoff event audit operations
     */
    public LabController(LabService labService, GroupService groupService, SignoffEventService signoffEventService) {
        this.labService = labService;
        this.groupService = groupService;
        this.signoffEventService = signoffEventService;
    }

    /**
     * Retrieves all labs.
     *
     * @return List of all labs
     */
    @GetMapping("/labs")
    public List<Lab> getLabs() {
        return labService.getAll();
    }

    /**
     * Creates or updates a lab.
     *
     * @param lab The lab to create or update
     * @return The created or updated lab
     */
    @PostMapping("/labs")
    public Lab createOrUpdateLab(@RequestBody Lab lab) {
        return labService.upsert(lab);
    }

    /**
     * Retrieves all groups associated with a specific lab.
     *
     * @param id The lab identifier
     * @return ResponseEntity containing the list of groups or an error message
     */
    @GetMapping("/labs/{id}/groups")
    public ResponseEntity<?> getGroupsByLabId(@PathVariable String id) {
        // Validate that the id is not empty
        if (id == null || id.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Lab ID cannot be empty");
        }

        // Check if lab exists
        try {
            if (!labService.labExists(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Lab with ID " + id + " not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking lab existence: " + e.getMessage());
        }

        // Retrieve groups for the lab
        try {
            List<Group> groups = groupService.getGroupsByLabId(id);
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving groups: " + e.getMessage());
        }
    }

    /**
     * Marks a group as passed for a specific lab.
     *
     * This endpoint updates the group's status to "passed" and creates an audit
     * entry in the signoff_events collection to track the action.
     *
     * @param labId   The lab identifier
     * @param groupId The group identifier
     * @return ResponseEntity containing the updated group or an error message
     */
    @PostMapping("/labs/{labId}/groups/{groupId}/pass")
    public ResponseEntity<?> passGroup(
            @PathVariable String labId,
            @PathVariable String groupId,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) String notes) {

        // Validate input parameters
        if (labId == null || labId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Lab ID cannot be empty");
        }
        if (groupId == null || groupId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Group ID cannot be empty");
        }

        // Check if lab exists
        try {
            if (!labService.labExists(labId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Lab with ID " + labId + " not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking lab existence: " + e.getMessage());
        }

        // Find the group
        Optional<Group> groupOptional;
        try {
            groupOptional = groupService.getAll().stream()
                    .filter(g -> g.getGroupId().equals(groupId) && g.getLabId().equals(labId))
                    .findFirst();

            if (groupOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Group with ID " + groupId + " not found in lab " + labId);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error finding group: " + e.getMessage());
        }

        // Update group status to "passed"
        Group group = groupOptional.get();
        try {
            group.setStatus("passed");
            groupService.upsert(group);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating group status: " + e.getMessage());
        }

        // Create audit entry in signoff_events collection
        try {
            String performer = (performedBy != null && !performedBy.trim().isEmpty())
                    ? performedBy : "system";
            SignoffEvent event = signoffEventService.createEvent(
                    labId,
                    groupId,
                    "PASS",
                    performer,
                    notes,
                    null
            );

            // Return success response with updated group
            return ResponseEntity.ok()
                    .body(new PassReturnResponse(
                            group,
                            event,
                            "Group successfully marked as passed"
                    ));
        } catch (Exception e) {
            // Even if audit logging fails, the group status was updated
            // Return success but log the audit failure
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .body("Group status updated but audit logging failed: " + e.getMessage());
        }
    }

    /**
     * Marks a group as returned (needs revision) for a specific lab.
     *
     * This endpoint updates the group's status to "returned" and creates an audit
     * entry in the signoff_events collection to track the action.
     *
     * @param labId   The lab identifier
     * @param groupId The group identifier
     * @return ResponseEntity containing the updated group or an error message
     */
    @PostMapping("/labs/{labId}/groups/{groupId}/return")
    public ResponseEntity<?> returnGroup(
            @PathVariable String labId,
            @PathVariable String groupId,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) String notes) {

        // Validate input parameters
        if (labId == null || labId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Lab ID cannot be empty");
        }
        if (groupId == null || groupId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Group ID cannot be empty");
        }

        // Check if lab exists
        try {
            if (!labService.labExists(labId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Lab with ID " + labId + " not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking lab existence: " + e.getMessage());
        }

        // Find the group
        Optional<Group> groupOptional;
        try {
            groupOptional = groupService.getAll().stream()
                    .filter(g -> g.getGroupId().equals(groupId) && g.getLabId().equals(labId))
                    .findFirst();

            if (groupOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Group with ID " + groupId + " not found in lab " + labId);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error finding group: " + e.getMessage());
        }

        // Update group status to "returned"
        Group group = groupOptional.get();
        try {
            group.setStatus("returned");
            groupService.upsert(group);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating group status: " + e.getMessage());
        }

        // Create audit entry in signoff_events collection
        try {
            String performer = (performedBy != null && !performedBy.trim().isEmpty())
                    ? performedBy : "system";
            SignoffEvent event = signoffEventService.createEvent(
                    labId,
                    groupId,
                    "RETURN",
                    performer,
                    notes,
                    null
            );

            // Return success response with updated group
            return ResponseEntity.ok()
                    .body(new PassReturnResponse(
                            group,
                            event,
                            "Group successfully marked as returned"
                    ));
        } catch (Exception e) {
            // Even if audit logging fails, the group status was updated
            // Return success but log the audit failure
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .body("Group status updated but audit logging failed: " + e.getMessage());
        }
    }

    /**
     * Response class for pass/return operations
     * Contains the updated group, the audit event, and a message
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

        public Group getGroup() {
            return group;
        }

        public SignoffEvent getEvent() {
            return event;
        }

        public String getMessage() {
            return message;
        }
    }
}