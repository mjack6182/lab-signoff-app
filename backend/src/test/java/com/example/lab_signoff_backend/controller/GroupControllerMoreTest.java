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
class GroupControllerMoreTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LabWebSocketController wsController;

    @MockBean
    private GroupRepository groupRepository;

    @Test
    void passNextCheckpoint_notFoundReturns404() throws Exception {
        when(groupRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(post("/groups/missing/pass"))
                .andExpect(status().isNotFound());
    }

    @Test
    void passNextCheckpoint_successPassesFirstReturnOrNull() throws Exception {
        Group group = new Group();
        group.setLabId("lab1");
        group.setGroupId("g1");
        CheckpointProgress cp1 = new CheckpointProgress();
        cp1.setCheckpointNumber(1);
        cp1.setStatus(SignoffAction.RETURN);
        CheckpointProgress cp2 = new CheckpointProgress();
        cp2.setCheckpointNumber(2);
        cp2.setStatus(null);
        group.setCheckpointProgress(List.of(cp1, cp2));
        when(groupRepository.findById("g1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/groups/g1/pass"))
                .andExpect(status().isOk());
    }
}
