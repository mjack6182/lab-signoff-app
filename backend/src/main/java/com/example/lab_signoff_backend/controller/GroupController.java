package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/group")
public class GroupController {

    @Autowired
    private LabWebSocketController wsController;

    // Test "Pass" endpoint
    @PostMapping("/{groupId}/pass")
    public void passCheckpoint(@PathVariable String groupId) {
        int checkpointNumber = 1;
        wsController.broadcastCheckpointUpdate(groupId, checkpointNumber, "PASS");
    }

    // Test "Return" endpoint
    @PostMapping("/{groupId}/return")
    public void returnCheckpoint(@PathVariable String groupId) {
        int checkpointNumber = 1;
        wsController.broadcastCheckpointUpdate(groupId, checkpointNumber, "RETURN");
    }
}
