package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Class;
import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import com.example.lab_signoff_backend.service.ClassService;
import com.example.lab_signoff_backend.service.GroupService;
import com.example.lab_signoff_backend.service.LabService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LabJoinController.class)
@AutoConfigureMockMvc(addFilters = false)
class LabJoinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LabService labService;
    @MockBean
    private ClassService classService;
    @MockBean
    private GroupService groupService;

    @Test
    void getLabByJoinCode_missing_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/labs/join/ "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLabByJoinCode_notFound_returns404() throws Exception {
        when(labService.getByJoinCode("BAD")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/labs/join/BAD"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLabByJoinCode_success_returnsLabAndClass() throws Exception {
        Lab lab = new Lab("class-1", "Lab 1", 2, "inst");
        lab.setId("lab-1");
        lab.setCheckpoints(List.of(new com.example.lab_signoff_backend.model.embedded.CheckpointDefinition()));
        Class clazz = new Class("CS101", "Intro", "Fall", "inst");
        clazz.setRoster(List.of("Student One"));
        when(labService.getByJoinCode("CODE")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));

        mockMvc.perform(get("/api/labs/join/CODE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labTitle").value("Lab 1"))
                .andExpect(jsonPath("$.students", hasSize(1)));
    }

    @Test
    void joinLab_missingStudentName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/labs/join/CODE/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinLab_notOnRoster_forbidden() throws Exception {
        Lab lab = new Lab("class-1", "Lab 1", 2, "inst");
        Class clazz = new Class("CS101", "Intro", "Fall", "inst");
        clazz.setRoster(List.of("Other"));
        when(labService.getByJoinCode("CODE")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));

        mockMvc.perform(post("/api/labs/join/CODE/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentName\":\"Student\",\"studentEmail\":\"s@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void joinLab_labNotFound_returns404() throws Exception {
        when(labService.getByJoinCode("CODE")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/labs/join/CODE/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentName\":\"Student\",\"studentEmail\":\"s@example.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void joinLab_classNotFound_returns404() throws Exception {
        Lab lab = new Lab("class-1", "Lab 1", 2, "inst");
        when(labService.getByJoinCode("CODE")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/labs/join/CODE/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentName\":\"Student\",\"studentEmail\":\"s@example.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLabDetail_emptyId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/labs/ "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGroupDetail_successWhenLabMatches() throws Exception {
        Lab lab = new Lab("class-1", "Lab 1", 2, "inst");
        lab.setId("lab-1");
        Class clazz = new Class("CS101", "Intro", "Fall", "inst");
        clazz.setId("class-1");
        Group group = new Group();
        group.setId("g-1");
        group.setLabId("lab-1");
        GroupMember member = new GroupMember();
        member.setName("Student");
        group.setMembers(List.of(member));
        when(labService.getById("lab-1")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getById("g-1")).thenReturn(Optional.of(group));

        mockMvc.perform(get("/api/labs/lab-1/groups/g-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(group.getGroupId()));
    }

    @Test
    void joinLab_success_createsGroupSummary() throws Exception {
        Lab lab = new Lab("class-1", "Lab 1", 2, "inst");
        Class clazz = new Class("CS101", "Intro", "Fall", "inst");
        clazz.setRoster(List.of("Student"));
        Group group = new Group();
        group.setId("g1");
        group.setGroupId("G-1");
        group.setLabId("lab-1");
        group.setStatus(GroupStatus.IN_PROGRESS);
        GroupMember member = new GroupMember();
        member.setUserId("Student");
        group.setMembers(List.of(member));

        when(labService.getByJoinCode("CODE")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getAll()).thenReturn(List.of());
        when(groupService.upsert(any(Group.class))).thenReturn(group);

        mockMvc.perform(post("/api/labs/join/CODE/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentName\":\"Student\",\"studentEmail\":\"s@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group.groupId").value("G-1"));
    }

    @Test
    void getGroupDetail_notFoundReturns404() throws Exception {
        when(labService.getById("lab-1")).thenReturn(Optional.of(new Lab("class-1", "Lab", 1, "inst")));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(new Class("CS", "Name", "Term", "inst")));
        when(groupService.getById("g-missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/labs/lab-1/groups/g-missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLabDetail_notFoundLab_returns404() throws Exception {
        when(labService.getById("bad")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/labs/bad"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLabByJoinCode_missingClass_returns404() throws Exception {
        Lab lab = new Lab("class-1", "Lab 1", 2, "inst");
        when(labService.getByJoinCode("CODE")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/labs/join/CODE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void joinLab_groupBelongsToDifferentLab_returnsNotFound() throws Exception {
        Lab lab = new Lab("class-1", "Lab 1", 2, "inst");
        lab.setId("lab-1");
        Class clazz = new Class("CS101", "Intro", "Fall", "inst");
        clazz.setRoster(List.of("Student"));
        Group group = new Group();
        group.setId("g-other");
        group.setLabId("other-lab");
        when(labService.getByJoinCode("CODE")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getById("g-other")).thenReturn(Optional.of(group));

        mockMvc.perform(get("/api/labs/lab-1/groups/g-other"))
                .andExpect(status().isNotFound());
    }

    @Test
    void joinLab_emptyJoinCode_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/labs/join/ /students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentName\":\"Student\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinLab_allowsRosterlessClass() throws Exception {
        Lab lab = new Lab("class-1", "Lab 1", 1, "inst");
        Class clazz = new Class("CS101", "Intro", "Fall", "inst");
        clazz.setRoster(null); // rosterless should allow join
        lab.setCheckpoints(List.of(new com.example.lab_signoff_backend.model.embedded.CheckpointDefinition()));
        when(labService.getByJoinCode("CODE")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-1")).thenReturn(List.of());
        when(groupService.upsert(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/labs/join/CODE/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentName\":\"Rosterless\",\"studentEmail\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group.members[0].name").value("Rosterless"));
    }

    @Test
    void joinLab_existingGroupDoesNotDuplicateMember() throws Exception {
        Lab lab = new Lab("class-1", "Lab 1", 1, "inst");
        lab.setId("lab-1");
        Class clazz = new Class("CS101", "Intro", "Fall", "inst");
        clazz.setId("class-1");
        clazz.setRoster(List.of("Student"));
        Group existing = new Group();
        existing.setLabId("lab-1");
        existing.setGroupId("G-1");
        GroupMember member = new GroupMember();
        member.setName("Student");
        existing.setMembers(List.of(member));
        existing.setId("G-1");

        when(labService.getByJoinCode("CODE")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId(anyString())).thenReturn(List.of(existing));

        mockMvc.perform(post("/api/labs/join/CODE/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentName\":\"Student\",\"studentEmail\":\"s@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group.groupId").value("G-1"));

        org.mockito.Mockito.verify(groupService, org.mockito.Mockito.never()).upsert(any(Group.class));
    }

    @Test
    void joinLab_createsDefaultEmailWhenMissing() throws Exception {
        Lab lab = new Lab("class-1", "Lab 1", 1, "inst");
        Class clazz = new Class("CS101", "Intro", "Fall", "inst");
        clazz.setId("class-1");
        clazz.setRoster(List.of("Student"));
        when(labService.getByJoinCode("CODE")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-1")).thenReturn(List.of());
        when(groupService.upsert(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/labs/join/CODE/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentName\":\"Student\",\"studentEmail\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group.members[0].email").value("student@students.local"));
    }

    @Test
    void getGroupDetail_blankIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/labs/ /groups/ ")).andExpect(status().isBadRequest());
    }

    @Test
    void getGroupDetail_returnsEmptyCollectionsWhenNull() throws Exception {
        Group group = new Group();
        group.setId("g-null");
        group.setLabId("lab-1");
        group.setGroupId("G-NULL");
        group.setMembers(null);
        group.setCheckpointProgress(null);
        when(groupService.getById("g-null")).thenReturn(Optional.of(group));

        mockMvc.perform(get("/api/labs/lab-1/groups/g-null"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(0)))
                .andExpect(jsonPath("$.checkpointProgress", hasSize(0)));
    }

    @Test
    void getLabDetail_classMissing_returnsNotFound() throws Exception {
        Lab lab = new Lab("c1", "Lab 1", 1, "inst");
        lab.setId("lab-1");
        when(labService.getById("lab-1")).thenReturn(Optional.of(lab));
        when(classService.getClassById("c1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/labs/lab-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void joinLab_nullRequest_returnsBadRequest() {
        LabJoinController controller = new LabJoinController(labService, classService, groupService);
        var response = controller.joinLab("code", null);
        org.junit.jupiter.api.Assertions.assertEquals(400, response.getStatusCode().value());
    }
}
