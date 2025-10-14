package com.example.lab_signoff_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.service.GroupService;
import com.example.lab_signoff_backend.service.LabService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * Constructor for LabController.
     *
     * @param labService   Service for lab operations
     * @param groupService Service for group operations
     */
    public LabController(LabService labService, GroupService groupService) {
        this.labService = labService;
        this.groupService = groupService;
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
}