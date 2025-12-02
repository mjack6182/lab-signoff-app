package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.HelpQueueItem;
import com.example.lab_signoff_backend.model.enums.HelpQueueStatus;
import com.example.lab_signoff_backend.service.HelpQueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HelpQueueController.class)
@AutoConfigureMockMvc(addFilters = false)
class HelpQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HelpQueueService helpQueueService;

    @Test
    void raiseHand_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/queue/labs/lab-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"help\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void raiseHand_conflictWhenActiveRequest_returns409() throws Exception {
        when(helpQueueService.raiseHand(anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("active"));

        mockMvc.perform(post("/api/queue/labs/lab-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"g1\",\"raisedBy\":\"u1\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void raiseHand_success_returnsCreatedItem() throws Exception {
        HelpQueueItem item = new HelpQueueItem("lab-1", "g1", "u1", 1);
        when(helpQueueService.raiseHand(eq("lab-1"), eq("g1"), eq("u1"), isNull()))
                .thenReturn(item);

        mockMvc.perform(post("/api/queue/labs/lab-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"g1\",\"raisedBy\":\"u1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId", is("g1")))
                .andExpect(jsonPath("$.labId", is("lab-1")));
    }

    @Test
    void getQueueItem_notFoundReturns404() throws Exception {
        when(helpQueueService.getQueueItem("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/queue/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getQueueForLab_defaultUsesActiveQueue() throws Exception {
        HelpQueueItem waiting = new HelpQueueItem("lab-1", "g1", "u1", 1);
        when(helpQueueService.getActiveQueue("lab-1")).thenReturn(List.of(waiting));

        mockMvc.perform(get("/api/queue/labs/lab-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value("g1"));
    }

    @Test
    void getQueueForLab_statusWaitingUsesWaitingQueue() throws Exception {
        HelpQueueItem waiting = new HelpQueueItem("lab-1", "g1", "u1", 1);
        when(helpQueueService.getWaitingQueue("lab-1")).thenReturn(List.of(waiting));

        mockMvc.perform(get("/api/queue/labs/lab-1").param("status", "waiting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value("g1"));
    }

    @Test
    void getQueueForLab_statusClaimedUsesClaimedQueue() throws Exception {
        HelpQueueItem claimed = new HelpQueueItem("lab-1", "g2", "u2", 2);
        claimed.setStatus(HelpQueueStatus.CLAIMED);
        when(helpQueueService.getClaimedQueue("lab-1")).thenReturn(List.of(claimed));

        mockMvc.perform(get("/api/queue/labs/lab-1").param("status", "claimed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value("g2"));
    }

    @Test
    void getQueueForLab_emptyStatusUsesActiveQueue() throws Exception {
        when(helpQueueService.getActiveQueue("lab-1")).thenReturn(List.of(new HelpQueueItem("lab-1", "g1", "u1", 1)));

        mockMvc.perform(get("/api/queue/labs/lab-1").param("status", ""))
                .andExpect(status().isOk());
    }

    @Test
    void getQueueForLab_unknownStatusUsesFallback() throws Exception {
        HelpQueueItem item = new HelpQueueItem("lab-1", "g3", "u3", 3);
        when(helpQueueService.getQueueForLab("lab-1")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/queue/labs/lab-1").param("status", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value("g3"));
    }

    @Test
    void claimRequest_badPayload_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/queue/item-1/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void claimRequest_runtimeErrorReturnsBadRequest() throws Exception {
        when(helpQueueService.claimRequest("missing", "u1")).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(put("/api/queue/missing/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"u1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void claimRequest_successReturnsItem() throws Exception {
        HelpQueueItem item = new HelpQueueItem("lab-1", "g1", "u1", 1);
        when(helpQueueService.claimRequest("item-1", "u1")).thenReturn(item);

        mockMvc.perform(put("/api/queue/item-1/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"u1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value("g1"));
    }

    @Test
    void resolveRequest_runtimeErrorReturnsBadRequest() throws Exception {
        when(helpQueueService.resolveRequest("missing")).thenThrow(new RuntimeException("missing"));

        mockMvc.perform(put("/api/queue/missing/resolve"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelRequest_runtimeErrorReturnsBadRequest() throws Exception {
        when(helpQueueService.cancelRequest("missing")).thenThrow(new RuntimeException("missing"));

        mockMvc.perform(delete("/api/queue/missing"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setUrgent_notFoundReturns404() throws Exception {
        when(helpQueueService.setUrgent("missing")).thenThrow(new RuntimeException("missing"));

        mockMvc.perform(put("/api/queue/missing/urgent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void setUrgent_successReturnsItem() throws Exception {
        HelpQueueItem item = new HelpQueueItem("lab-1", "g1", "u1", 1);
        when(helpQueueService.setUrgent("item-1")).thenReturn(item);

        mockMvc.perform(put("/api/queue/item-1/urgent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value("g1"));
    }

    @Test
    void getClaimedByUser_exceptionReturns500() throws Exception {
        when(helpQueueService.getClaimedByUser("uX")).thenThrow(new RuntimeException("fail"));

        mockMvc.perform(get("/api/queue/user/uX/claimed"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getRaisedByUser_exceptionReturns500() throws Exception {
        when(helpQueueService.getRaisedByUser("uY")).thenThrow(new RuntimeException("fail"));

        mockMvc.perform(get("/api/queue/user/uY/raised"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getQueueStats_returnsCounts() throws Exception {
        when(helpQueueService.countWaitingItems("lab-1")).thenReturn(2L);
        when(helpQueueService.countActiveItems("lab-1")).thenReturn(3L);

        mockMvc.perform(get("/api/queue/labs/lab-1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waiting").value(2))
                .andExpect(jsonPath("$.active").value(3))
                .andExpect(jsonPath("$.claimed").value(1));
    }

    @Test
    void checkActiveRequest_returnsFlags() throws Exception {
        HelpQueueItem item = new HelpQueueItem("lab-1", "g1", "u1", 1);
        when(helpQueueService.hasActiveRequest("lab-1", "g1")).thenReturn(true);
        when(helpQueueService.getActiveRequestForGroup("lab-1", "g1")).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/queue/labs/lab-1/groups/g1/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActiveRequest").value(true))
                .andExpect(jsonPath("$.activeRequest.groupId").value("g1"));
    }
}
