package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.repository.GroupRepository;
import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private final LabWebSocketController wsController;
    private final GroupRepository groupRepository;

    // ✅ Constructor-based dependency injection (best practice)
    @Autowired
    public GroupController(LabWebSocketController wsController, GroupRepository groupRepository) {
        this.wsController = wsController;
        this.groupRepository = groupRepository;
    }

    // ✅ GET all groups (matches your frontend)
    @GetMapping
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    // ✅ GET a specific group by ID (optional)
    @GetMapping("/{groupId}")
    public Group getGroupById(@PathVariable String groupId) {
        return groupRepository.findById(groupId).orElse(null);
    }

    // ✅ WebSocket test endpoints
    @PostMapping("/{groupId}/pass")
    public void passCheckpoint(@PathVariable String groupId) {
        wsController.broadcastCheckpointUpdate(groupId, 1, "PASS");
    }

    @PostMapping("/{groupId}/return")
    public void returnCheckpoint(@PathVariable String groupId) {
        wsController.broadcastCheckpointUpdate(groupId, 1, "RETURN");
    }
}
