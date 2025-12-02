package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestBroadcastController.class)
@AutoConfigureMockMvc(addFilters = false)
class TestBroadcastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LabWebSocketController wsController;

    @Test
    void sendTest_triggersBroadcast() throws Exception {
        mockMvc.perform(get("/test-broadcast"))
                .andExpect(status().isOk());

        verify(wsController).broadcastCheckpointUpdate(eq("lab1"), org.mockito.ArgumentMatchers.any());
    }
}
