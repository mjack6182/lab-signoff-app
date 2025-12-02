package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.repository.GroupRepository;
import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerAdditionalTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LabWebSocketController wsController;

    @MockBean
    private GroupRepository groupRepository;

    @Test
    void toggleCheckpoint_missingCheckpoint_returnsBadRequest() throws Exception {
        Group group = new Group();
        group.setGroupId("g1");
        group.setLabId("lab1");
        group.setCheckpointProgress(List.of()); // no checkpoints
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));

        mockMvc.perform(post("/groups/g1/checkpoints/1/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true,\"performedBy\":\"ta\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void toggleCheckpoint_success_updatesStatus() throws Exception {
        Group group = new Group();
        group.setGroupId("g1");
        group.setLabId("lab1");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.RETURN);
        group.setCheckpointProgress(List.of(cp));
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/groups/g1/checkpoints/1/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true,\"performedBy\":\"ta\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void toggleCheckpoint_groupNotFound_returns404() throws Exception {
        when(groupRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(post("/groups/missing/checkpoints/1/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleCheckpoint_checkpointMissing_returnsBadRequest() throws Exception {
        Group group = new Group();
        group.setGroupId("g2");
        group.setLabId("lab1");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(2);
        group.setCheckpointProgress(List.of(cp));
        when(groupRepository.findById("g2")).thenReturn(Optional.of(group));

        mockMvc.perform(post("/groups/g2/checkpoints/5/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passNextCheckpoint_noCheckpoints_returnsBadRequest() throws Exception {
        Group group = new Group();
        group.setGroupId("g3");
        group.setLabId("lab1");
        group.setCheckpointProgress(List.of());
        when(groupRepository.findById("g3")).thenReturn(Optional.of(group));

        mockMvc.perform(post("/groups/g3/pass"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passNextCheckpoint_allPassed_returnsBadRequest() throws Exception {
        Group group = new Group();
        group.setGroupId("g4");
        group.setLabId("lab1");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));
        when(groupRepository.findById("g4")).thenReturn(Optional.of(group));

        mockMvc.perform(post("/groups/g4/pass"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void toggleCheckpoint_nullCheckpoints_returnsBadRequest() throws Exception {
        Group group = new Group();
        group.setGroupId("g5");
        group.setLabId("lab1");
        group.setCheckpointProgress(null);
        when(groupRepository.findById("g5")).thenReturn(Optional.of(group));

        mockMvc.perform(post("/groups/g5/checkpoints/1/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passNextCheckpoint_successReturnsOk() throws Exception {
        Group group = new Group();
        group.setGroupId("g6");
        group.setLabId("lab1");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.RETURN);
        group.setCheckpointProgress(List.of(cp));
        when(groupRepository.findById("g6")).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        mockMvc.perform(post("/groups/g6/pass"))
                .andExpect(status().isOk());
    }
}
