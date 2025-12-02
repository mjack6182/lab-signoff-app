package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LabWebSocketController.class)
@AutoConfigureMockMvc(addFilters = false)
class LabWebSocketControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimpMessagingTemplate template;

    @Test
    void wsTestBroadcast_endpointReturnsOk() throws Exception {
        mockMvc.perform(get("/ws-test-broadcast"))
                .andExpect(status().isOk());
    }
}
