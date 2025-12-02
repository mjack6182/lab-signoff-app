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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LabWebSocketController wsController;

    @MockBean
    private GroupRepository groupRepository;

    @Test
    void getGroupById_notFoundReturns404() throws Exception {
        when(groupRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/groups/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllGroups_returnsList() throws Exception {
        when(groupRepository.findAll()).thenReturn(List.of(new Group()));

        mockMvc.perform(get("/groups"))
                .andExpect(status().isOk());
    }

    @Test
    void toggleCheckpoint_missingCheckpointReturnsBadRequest() throws Exception {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        group.setCheckpointProgress(List.of()); // empty
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));

        mockMvc.perform(post("/groups/g1/checkpoints/1/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true,\"performedBy\":\"ta\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passNextCheckpoint_noRemaining_returnsBadRequest() throws Exception {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));

        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));

        mockMvc.perform(post("/groups/g1/pass"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("already passed")));
    }

    @Test
    void passNextCheckpoint_marksNextAndBroadcasts() throws Exception {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        CheckpointProgress cp1 = new CheckpointProgress();
        cp1.setCheckpointNumber(1);
        cp1.setStatus(SignoffAction.RETURN);
        group.setCheckpointProgress(List.of(cp1));

        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/groups/g1/pass"))
                .andExpect(status().isOk());

        verify(wsController).broadcastCheckpointUpdate(eq("lab1"), any());
    }

    @Test
    void toggleCheckpoint_successUpdatesPass() throws Exception {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        CheckpointProgress cp1 = new CheckpointProgress();
        cp1.setCheckpointNumber(1);
        cp1.setStatus(SignoffAction.RETURN);
        group.setCheckpointProgress(List.of(cp1));
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/groups/g1/checkpoints/1/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true,\"performedBy\":\"ta\",\"notes\":\"n\"}"))
                .andExpect(status().isOk());

        verify(groupRepository).save(any(Group.class));
        verify(wsController).broadcastCheckpointUpdate(eq("lab1"), any());
    }

    @Test
    void toggleCheckpoint_setsReturnWhenCompletedFalse() throws Exception {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        CheckpointProgress cp1 = new CheckpointProgress();
        cp1.setCheckpointNumber(1);
        cp1.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp1));
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/groups/g1/checkpoints/1/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":false,\"performedBy\":\"ta\",\"notes\":\"redo\"}"))
                .andExpect(status().isOk());

        verify(groupRepository).save(any(Group.class));
    }
}
