package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.service.GroupService;
import com.example.lab_signoff_backend.service.LabService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LabController {

    private final LabService labService;
    private final GroupService groupService;

    public LabController(LabService labService, GroupService groupService) {
        this.labService = labService;
        this.groupService = groupService;
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