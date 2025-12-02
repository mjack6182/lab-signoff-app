package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.SignoffEvent;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.repository.GroupRepository;
import com.example.lab_signoff_backend.service.SignoffEventService;
import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LabGroupControllerTest {

    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final SignoffEventService signoffEventService = mock(SignoffEventService.class);
    private final LabWebSocketController wsController = mock(LabWebSocketController.class);

    @Test
    void passCheckpoint_groupNotFound_returns404() {
        when(groupRepository.findByGroupId("g1")).thenReturn(Optional.empty());
        when(groupRepository.findById("g1")).thenReturn(Optional.empty());
        LabGroupController controller = new LabGroupController(groupRepository, signoffEventService, wsController);

        ResponseEntity<?> resp = controller.passCheckpointPersisted("lab1", "g1", Map.of());

        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void passCheckpoint_missingCheckpoints_returnsBadRequest() {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        when(groupRepository.findByGroupId("g1")).thenReturn(Optional.of(group));
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));
        LabGroupController controller = new LabGroupController(groupRepository, signoffEventService, wsController);

        ResponseEntity<?> resp = controller.passCheckpointPersisted("lab1", "g1", Map.of("checkpointNumber", 1));

        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void passCheckpoint_success_persistsAndBroadcasts() {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.RETURN);
        group.setCheckpointProgress(List.of(cp));

        when(groupRepository.findByGroupId("g1")).thenReturn(Optional.of(group));
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
        SignoffEvent event = new SignoffEvent();
        event.setId("evt1");
        when(signoffEventService.createEvent(any(SignoffEvent.class))).thenReturn(event);

        LabGroupController controller = new LabGroupController(groupRepository, signoffEventService, wsController);

        ResponseEntity<?> resp = controller.passCheckpointPersisted("lab1", "g1", Map.of("checkpointNumber", 1, "performedBy", "ta"));

        assertEquals(200, resp.getStatusCode().value());
        verify(wsController).broadcastCheckpointUpdate(eq("lab1"), any());
    }

    @Test
    void returnCheckpoint_success_marksReturn() {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));

        when(groupRepository.findByGroupId("g1")).thenReturn(Optional.of(group));
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
        SignoffEvent event = new SignoffEvent();
        event.setId("evt2");
        when(signoffEventService.createEvent(any(SignoffEvent.class))).thenReturn(event);

        LabGroupController controller = new LabGroupController(groupRepository, signoffEventService, wsController);

        ResponseEntity<?> resp = controller.returnCheckpointPersisted("lab1", "g1", Map.of("checkpointNumber", 1, "performedBy", "ta"));

        assertEquals(200, resp.getStatusCode().value());
        verify(wsController).broadcastCheckpointUpdate(eq("lab1"), any());
    }

    @Test
    void returnCheckpoint_missingCheckpointReturnsBadRequest() {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        group.setCheckpointProgress(List.of());
        when(groupRepository.findByGroupId("g1")).thenReturn(Optional.of(group));
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));

        LabGroupController controller = new LabGroupController(groupRepository, signoffEventService, wsController);
        ResponseEntity<?> resp = controller.returnCheckpointPersisted("lab1", "g1", Map.of("checkpointNumber", 1));
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void passCheckpoint_withMissingCheckpointReturnsBadRequest() {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        group.setCheckpointProgress(List.of());
        when(groupRepository.findByGroupId("g1")).thenReturn(Optional.of(group));
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));

        LabGroupController controller = new LabGroupController(groupRepository, signoffEventService, wsController);
        ResponseEntity<?> resp = controller.passCheckpointPersisted("lab1", "g1", Map.of("checkpointNumber", 1));
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void passCheckpoint_invalidStatusReturnsBadRequest() {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.RETURN);
        group.setCheckpointProgress(List.of(cp));
        when(groupRepository.findByGroupId("g1")).thenReturn(Optional.of(group));
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));

        LabGroupController controller = new LabGroupController(groupRepository, signoffEventService, wsController);
        ResponseEntity<?> resp = controller.passCheckpointPersisted("lab1", "g1", Map.of("checkpointNumber", 1, "status", "BOGUS"));
        // invalid status should bubble RuntimeException -> 500
        assertEquals(500, resp.getStatusCode().value());
    }

    @Test
    void passCheckpoint_usesRepositoryIdFallback() {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(2);
        cp.setStatus(SignoffAction.RETURN);
        group.setCheckpointProgress(List.of(cp));

        when(groupRepository.findByGroupId("g1")).thenReturn(Optional.empty());
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
        SignoffEvent event = new SignoffEvent();
        event.setId("evt-fallback-pass");
        when(signoffEventService.createEvent(any(SignoffEvent.class))).thenReturn(event);

        LabGroupController controller = new LabGroupController(groupRepository, signoffEventService, wsController);
        ResponseEntity<?> resp = controller.passCheckpointPersisted("lab1", "g1", Map.of("checkpointNumber", 2));

        assertEquals(200, resp.getStatusCode().value());
        verify(wsController).broadcastCheckpointUpdate(eq("lab1"), any());
    }

    @Test
    void returnCheckpoint_usesRepositoryIdFallback() {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(2);
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));

        when(groupRepository.findByGroupId("g1")).thenReturn(Optional.empty());
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
        SignoffEvent event = new SignoffEvent();
        event.setId("evt-fallback-return");
        when(signoffEventService.createEvent(any(SignoffEvent.class))).thenReturn(event);

        LabGroupController controller = new LabGroupController(groupRepository, signoffEventService, wsController);
        ResponseEntity<?> resp = controller.returnCheckpointPersisted("lab1", "g1", Map.of("checkpointNumber", 2));

        assertEquals(200, resp.getStatusCode().value());
        verify(wsController).broadcastCheckpointUpdate(eq("lab1"), any());
    }
}
