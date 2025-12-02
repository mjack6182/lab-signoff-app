package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.SignoffEvent;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import com.example.lab_signoff_backend.service.GroupService;
import com.example.lab_signoff_backend.service.LabService;
import com.example.lab_signoff_backend.service.SignoffEventService;
import com.example.lab_signoff_backend.websocket.LabWebSocketController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LabController.class)
@AutoConfigureMockMvc(addFilters = false) // disable security filters for focused controller tests
class LabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LabService labService;
    @MockBean
    private GroupService groupService;
    @MockBean
    private SignoffEventService signoffEventService;
    @MockBean
    private LabWebSocketController wsController;

    @Test
    void getGroupsByLabId_whenEmpty_returnsBadRequest_directCall() {
        LabController controller = new LabController(labService, groupService, signoffEventService, wsController);
        var response = controller.getGroupsByLabId("  ");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getGroupsByLabId_whenNotFound_returns404() throws Exception {
        when(labService.labExists("missing")).thenReturn(false);

        mockMvc.perform(get("/lti/labs/missing/groups"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value(containsString("not found")));
    }

    @Test
    void getGroupsByLabId_success_returnsGroups() throws Exception {
        Group g = buildGroup("lab-1", "group-1");
        when(labService.labExists("lab-1")).thenReturn(true);
        when(groupService.getGroupsByLabId("lab-1")).thenReturn(List.of(g));

        mockMvc.perform(get("/lti/labs/lab-1/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].groupId").value("group-1"));
    }

    @Test
    void passGroup_whenLabMissing_returns404() throws Exception {
        when(labService.labExists("lab-x")).thenReturn(false);

        mockMvc.perform(post("/lti/labs/lab-x/groups/group-y/pass"))
                .andExpect(status().isNotFound());
    }

    @Test
    void passGroup_whenGroupMissing_returns404() throws Exception {
        when(labService.labExists("lab-1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(List.of(buildGroup("lab-1", "other")));

        mockMvc.perform(post("/lti/labs/lab-1/groups/group-1/pass"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value(containsString("not found")));
    }

    @Test
    void passGroup_success_setsStatusAndBroadcasts() throws Exception {
        Group existing = buildGroup("lab-1", "group-1");
        when(labService.labExists("lab-1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(List.of(existing));
        SignoffEvent event = new SignoffEvent();
        event.setId("evt-1");
        when(signoffEventService.createEvent(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(event);

        mockMvc.perform(post("/lti/labs/lab-1/groups/group-1/pass")
                        .param("performedBy", "prof")
                        .param("notes", "nice work")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("passed")))
                .andExpect(jsonPath("$.event.id").value("evt-1"));

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupService).upsert(captor.capture());
        assertEquals(GroupStatus.SIGNED_OFF, captor.getValue().getStatus());
        verify(wsController).broadcastGroupPassed("lab-1", "group-1");
    }

    @Test
    void passGroup_badIds_returnBadRequest() throws Exception {
        mockMvc.perform(post("/lti/labs/ /groups/g1/pass"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/lti/labs/lab1/groups/ /pass"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnGroup_labMissing_returns404() throws Exception {
        when(labService.labExists("lab-x")).thenReturn(false);

        mockMvc.perform(post("/lti/labs/lab-x/groups/g1/return"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnGroup_groupMissing_returns404() throws Exception {
        when(labService.labExists("lab-1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(List.of(buildGroup("lab-1", "other")));

        mockMvc.perform(post("/lti/labs/lab-1/groups/g1/return"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnGroup_success_setsInProgress() throws Exception {
        Group existing = buildGroup("lab-1", "group-2");
        when(labService.labExists("lab-1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(List.of(existing));
        when(signoffEventService.createEvent(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(new SignoffEvent());

        mockMvc.perform(post("/lti/labs/lab-1/groups/group-2/return")
                        .param("notes", "fix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("returned")));

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupService).upsert(captor.capture());
        assertEquals(GroupStatus.IN_PROGRESS, captor.getValue().getStatus());
    }

    @Test
    void getLabs_returnsList() throws Exception {
        Lab lab = new Lab("class-1", "Lab 1", 2, "inst");
        when(labService.getAll()).thenReturn(List.of(lab));

        mockMvc.perform(get("/lti/labs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Lab 1"));
    }

    @Test
    void randomizeGroups_emptyLabId_returnsBadRequest() {
        LabController controller = new LabController(labService, groupService, signoffEventService, wsController);
        var response = controller.randomizeGroups("  ");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void randomizeGroups_labNotFound_returns404() {
        when(labService.labExists("missing-lab")).thenReturn(false);
        LabController controller = new LabController(labService, groupService, signoffEventService, wsController);

        var response = controller.randomizeGroups("missing-lab");
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateGroups_runtimeExceptionReturnsBadRequest() {
        when(labService.labExists("lab-err")).thenReturn(true);
        when(groupService.bulkUpdateGroups(eq("lab-err"), anyList())).thenThrow(new RuntimeException("boom"));

        LabController controller = new LabController(labService, groupService, signoffEventService, wsController);
        var response = controller.updateGroups("lab-err", List.of(buildGroup("lab-err", "g1")));

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("boom"));
    }

    @Test
    void passGroup_usesPayloadValues() throws Exception {
        Group existing = buildGroup("lab-1", "group-9");
        when(labService.labExists("lab-1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(List.of(existing));
        SignoffEvent event = new SignoffEvent();
        event.setId("evt-9");
        when(signoffEventService.createEvent(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(event);

        mockMvc.perform(post("/lti/labs/lab-1/groups/group-9/pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"performedBy\":\"teacher\",\"notes\":\"good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.id").value("evt-9"));

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupService).upsert(captor.capture());
        assertEquals("group-9", captor.getValue().getGroupId());
        verify(wsController).broadcastGroupPassed("lab-1", "group-9");
    }

    @Test
    void returnGroup_usesPayloadValues() throws Exception {
        Group existing = buildGroup("lab-1", "group-8");
        when(labService.labExists("lab-1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(List.of(existing));
        when(signoffEventService.createEvent(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(new SignoffEvent());

        mockMvc.perform(post("/lti/labs/lab-1/groups/group-8/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"performedBy\":\"teacher\",\"notes\":\"redo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("returned")));

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupService).upsert(captor.capture());
        assertEquals(GroupStatus.IN_PROGRESS, captor.getValue().getStatus());
    }

    @Test
    void getGroupsByLabId_nullId_returnsBadRequest() {
        LabController controller = new LabController(labService, groupService, signoffEventService, wsController);
        var response = controller.getGroupsByLabId(null);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void randomizeGroups_successReturnsPayloadAndBroadcasts() throws Exception {
        Group g1 = buildGroup("lab-1", "g1");
        Group g2 = buildGroup("lab-1", "g2");
        when(labService.labExists("lab-1")).thenReturn(true);
        when(groupService.randomizeGroups("lab-1")).thenReturn(List.of(g1, g2));

        LabController controller = new LabController(labService, groupService, signoffEventService, wsController);
        var response = controller.randomizeGroups("lab-1");

        assertEquals(200, response.getStatusCode().value());
        Object body = response.getBody();
        int count = (int) body.getClass().getMethod("getGroupCount").invoke(body);
        @SuppressWarnings("unchecked")
        List<Group> groups = (List<Group>) body.getClass().getMethod("getGroups").invoke(body);
        String message = (String) body.getClass().getMethod("getMessage").invoke(body);
        assertEquals(2, count);
        assertTrue(message.toLowerCase().contains("randomized"));
        assertEquals("g1", groups.get(0).getGroupId());
        verify(wsController).broadcastGroupsRandomized("lab-1");
    }

    @Test
    void randomizeGroups_runtimeExceptionReturnsBadRequest() throws Exception {
        when(labService.labExists("lab-err")).thenReturn(true);
        when(groupService.randomizeGroups("lab-err")).thenThrow(new RuntimeException("cannot randomize"));

        LabController controller = new LabController(labService, groupService, signoffEventService, wsController);
        var response = controller.randomizeGroups("lab-err");

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("cannot randomize"));
    }

    @Test
    void updateGroups_successBroadcastsAndReturnsResponse() throws Exception {
        Group g1 = buildGroup("lab-2", "g1");
        when(labService.labExists("lab-2")).thenReturn(true);
        when(groupService.bulkUpdateGroups(eq("lab-2"), anyList())).thenReturn(List.of(g1));

        LabController controller = new LabController(labService, groupService, signoffEventService, wsController);
        var response = controller.updateGroups("lab-2", List.of(g1));

        assertEquals(200, response.getStatusCode().value());
        Object body = response.getBody();
        int count = (int) body.getClass().getMethod("getGroupCount").invoke(body);
        @SuppressWarnings("unchecked")
        List<Group> groups = (List<Group>) body.getClass().getMethod("getGroups").invoke(body);
        String message = (String) body.getClass().getMethod("getMessage").invoke(body);
        assertEquals(1, count);
        assertEquals("g1", groups.get(0).getGroupId());
        assertTrue(message.toLowerCase().contains("updated"));

        verify(wsController).broadcastGroupsRandomized("lab-2");
    }

    @Test
    void updateGroups_missingLab_returnsNotFound() throws Exception {
        when(labService.labExists("missing-lab")).thenReturn(false);
        LabController controller = new LabController(labService, groupService, signoffEventService, wsController);

        var response = controller.updateGroups("missing-lab", List.of(buildGroup("missing-lab", "g1")));
        assertEquals(404, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("not found"));
    }

    @Test
    void updateGroups_withNullList_returnsBadRequest() {
        LabController controller = new LabController(labService, groupService, signoffEventService, wsController);
        var response = controller.updateGroups("lab-3", null);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void updateGroups_blankLabId_returnsBadRequest() {
        LabController controller = new LabController(labService, groupService, signoffEventService, wsController);
        var response = controller.updateGroups("  ", List.of());
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void passGroup_defaultsPerformedByWhenPayloadMissing() throws Exception {
        Group existing = buildGroup("lab-1", "group-11");
        when(labService.labExists("lab-1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(List.of(existing));
        SignoffEvent event = new SignoffEvent();
        event.setId("evt-11");
        when(signoffEventService.createEvent(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(event);

        mockMvc.perform(post("/lti/labs/lab-1/groups/group-11/pass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event.id").value("evt-11"));

        verify(wsController).broadcastGroupPassed("lab-1", "group-11");
    }

    @Test
    void returnGroup_defaultsWhenPayloadMissing() throws Exception {
        Group existing = buildGroup("lab-1", "group-12");
        when(labService.labExists("lab-1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(List.of(existing));
        when(signoffEventService.createEvent(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(new SignoffEvent());

        mockMvc.perform(post("/lti/labs/lab-1/groups/group-12/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("returned")));
    }

    @Test
    void passGroup_payloadWithoutPerformer_usesSystemDefault() throws Exception {
        Group existing = buildGroup("lab-1", "group-10");
        when(labService.labExists("lab-1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(List.of(existing));
        when(signoffEventService.createEvent(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(new SignoffEvent());

        mockMvc.perform(post("/lti/labs/lab-1/groups/group-10/pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"only\"}"))
                .andExpect(status().isOk());

        verify(wsController).broadcastGroupPassed("lab-1", "group-10");
    }

    @Test
    void returnGroup_badIds_returnBadRequest() throws Exception {
        mockMvc.perform(post("/lti/labs/ /groups/g1/return"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/lti/labs/lab1/groups/ /return"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passGroup_emptyPayload_defaultsValues() throws Exception {
        Group existing = buildGroup("lab-2", "group-13");
        when(labService.labExists("lab-2")).thenReturn(true);
        when(groupService.getAll()).thenReturn(List.of(existing));
        when(signoffEventService.createEvent(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(new SignoffEvent());

        mockMvc.perform(post("/lti/labs/lab-2/groups/group-13/pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void returnGroup_emptyPayload_defaultsValues() throws Exception {
        Group existing = buildGroup("lab-2", "group-14");
        when(labService.labExists("lab-2")).thenReturn(true);
        when(groupService.getAll()).thenReturn(List.of(existing));
        when(signoffEventService.createEvent(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(new SignoffEvent());

        mockMvc.perform(post("/lti/labs/lab-2/groups/group-14/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    private static Group buildGroup(String labId, String groupId) {
        Group g = new Group();
        g.setLabId(labId);
        g.setGroupId(groupId);
        g.setStatus(GroupStatus.IN_PROGRESS);
        return g;
    }
}
