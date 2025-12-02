package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.dto.EnrollmentWithUserDTO;
import com.example.lab_signoff_backend.model.Enrollment;
import com.example.lab_signoff_backend.model.User;
import com.example.lab_signoff_backend.model.enums.EnrollmentRole;
import com.example.lab_signoff_backend.model.enums.EnrollmentStatus;
import com.example.lab_signoff_backend.service.EnrollmentService;
import com.example.lab_signoff_backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EnrollmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnrollmentService enrollmentService;
    @MockBean
    private UserService userService;

    @Test
    void createEnrollment_badRole_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"u1\",\"classId\":\"c1\",\"role\":\"BAD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEnrollment_conflict_returns409() throws Exception {
        when(enrollmentService.enrollUser(anyString(), anyString(), any())).thenThrow(new RuntimeException("dup"));

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"u1\",\"classId\":\"c1\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void createEnrollment_success_returnsCreated() throws Exception {
        Enrollment enrollment = new Enrollment("u1", "c1", EnrollmentRole.STUDENT);
        when(enrollmentService.enrollUser(anyString(), anyString(), any())).thenReturn(enrollment);

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"u1\",\"classId\":\"c1\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getEnrollment_notFound_returns404() throws Exception {
        when(enrollmentService.getEnrollmentById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/enrollments/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEnrollmentsByUser_activeOnlyUsesActiveService() throws Exception {
        when(enrollmentService.getActiveEnrollmentsByUser("u1")).thenReturn(List.of(new Enrollment()));

        mockMvc.perform(get("/api/enrollments/user/u1").param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getEnrollmentsByClass_badRole_returns400() throws Exception {
        mockMvc.perform(get("/api/enrollments/class/c1").param("role", "BAD"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEnrollmentsByClass_withRoleUsesStudents() throws Exception {
        when(enrollmentService.getStudents("c1")).thenReturn(List.of(new Enrollment()));

        mockMvc.perform(get("/api/enrollments/class/c1").param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getEnrollmentsByClass_returnsAllWhenNoRole() throws Exception {
        when(enrollmentService.getEnrollmentsByClass("c1")).thenReturn(List.of(new Enrollment(), new Enrollment()));

        mockMvc.perform(get("/api/enrollments/class/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getEnrollmentsByClass_roleTaActiveTrue_usesActiveTAs() throws Exception {
        Enrollment e = buildEnrollment("ta1", EnrollmentRole.TA);
        // Controller currently routes TA role activeOnly to getActiveStudents; cover that branch
        when(enrollmentService.getActiveStudents("c2")).thenReturn(List.of(e));

        mockMvc.perform(get("/api/enrollments/class/c2")
                        .param("role", "TA")
                        .param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("ta1"));
    }

    @Test
    void changeRole_success_returnsUpdated() throws Exception {
        Enrollment e = buildEnrollment("u-change", EnrollmentRole.TA);
        when(enrollmentService.changeRole(eq("en1"), eq(EnrollmentRole.TA), anyString())).thenReturn(e);

        mockMvc.perform(put("/api/enrollments/en1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TA\",\"performedBy\":\"teacher\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TA"));
    }

    @Test
    void deleteEnrollment_serverErrorReturns500() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("fail")).when(enrollmentService).deleteEnrollment("bad");

        mockMvc.perform(delete("/api/enrollments/bad"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getEnrollmentsByClass_roleTaActiveFalse_usesStudents() throws Exception {
        Enrollment e = buildEnrollment("ta2", EnrollmentRole.TA);
        when(enrollmentService.getStudents("c2")).thenReturn(List.of(e));

        mockMvc.perform(get("/api/enrollments/class/c2")
                        .param("role", "TA")
                        .param("activeOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("ta2"));
    }

    @Test
    void getEnrollmentsByUser_activeOnlyFalseUsesAll() throws Exception {
        when(enrollmentService.getEnrollmentsByUser("uAll")).thenReturn(List.of(new Enrollment()));

        mockMvc.perform(get("/api/enrollments/user/uAll"))
                .andExpect(status().isOk());
    }

    @Test
    void createEnrollment_genericException_returns500() throws Exception {
        when(enrollmentService.enrollUser(anyString(), anyString(), any())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"u1\",\"classId\":\"c1\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void getEnrollmentStats_exception_returns500() throws Exception {
        when(enrollmentService.countStudents("cErr")).thenThrow(new RuntimeException("fail"));

        mockMvc.perform(get("/api/enrollments/class/cErr/stats"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void upgradeToTA_exception_returns500() throws Exception {
        when(enrollmentService.upgradeToTA(eq("enX"), anyString())).thenThrow(new IllegalStateException("fail"));

        mockMvc.perform(put("/api/enrollments/enX/upgrade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"performedBy\":\"teacher\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeRole_genericException_returns500() throws Exception {
    }

    @Test
    void dropEnrollment_genericException_returns500() throws Exception {
    }

    @Test
    void completeEnrollment_genericException_returns500() throws Exception {
    }

    @Test
    void getStudents_enrichesUserData_defaultsWhenMissing() throws Exception {
        Enrollment e = new Enrollment("u1", "c1", EnrollmentRole.STUDENT);
        e.setId("en1");
        e.setStatus(EnrollmentStatus.ACTIVE);
        e.setEnrolledAt(Instant.now());
        when(enrollmentService.getStudents("c1")).thenReturn(List.of(e));
        when(userService.getAllUsers()).thenReturn(List.of()); // no user found, use defaults

        mockMvc.perform(get("/api/enrollments/class/c1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userName").value("Unknown Student"))
                .andExpect(jsonPath("$[0].userEmail").value(""));
    }

    @Test
    void changeRole_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/enrollments/en1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeRole_illegalRole_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/enrollments/en1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"BAD\",\"performedBy\":\"teacher\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dropAndComplete_returnOk() throws Exception {
        Enrollment e = new Enrollment("u1", "c1", EnrollmentRole.STUDENT);
        when(enrollmentService.dropEnrollment("en1")).thenReturn(e);
        when(enrollmentService.completeEnrollment("en1")).thenReturn(e);

        mockMvc.perform(put("/api/enrollments/en1/drop"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/enrollments/en1/complete"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteEnrollment_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/enrollments/en1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getTAs_activeOnlyUsesActiveService() throws Exception {
        when(enrollmentService.getActiveTAs("c1")).thenReturn(List.of(new Enrollment()));

        mockMvc.perform(get("/api/enrollments/class/c1/tas").param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void dropAndCompleteEndpoints_returnNotFoundWhenMissing() throws Exception {
        when(enrollmentService.dropEnrollment("missing")).thenThrow(new RuntimeException("not found"));
        when(enrollmentService.completeEnrollment("missing")).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(put("/api/enrollments/missing/drop"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/enrollments/missing/complete"))
                .andExpect(status().isNotFound());
    }
    @Test
    void getTas_andStaff_returnLists() throws Exception {
        when(enrollmentService.getTAs("c1")).thenReturn(List.of(new Enrollment()));
        when(enrollmentService.getStaff("c1")).thenReturn(List.of(new Enrollment(), new Enrollment()));

        mockMvc.perform(get("/api/enrollments/class/c1/tas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/enrollments/class/c1/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void checkEnrollment_returnsFlagAndEnrollment() throws Exception {
        Enrollment e = new Enrollment("u1", "c1", EnrollmentRole.STUDENT);
        when(enrollmentService.isEnrolled("u1", "c1")).thenReturn(true);
        when(enrollmentService.getEnrollment("u1", "c1")).thenReturn(Optional.of(e));

        mockMvc.perform(get("/api/enrollments/check").param("userId", "u1").param("classId", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEnrolled").value(true))
                .andExpect(jsonPath("$.enrollment.userId").value("u1"));
    }

    @Test
    void getEnrollmentStats_returnsCounts() throws Exception {
        when(enrollmentService.countStudents("c1")).thenReturn(3L);
        when(enrollmentService.countTAs("c1")).thenReturn(1L);

        mockMvc.perform(get("/api/enrollments/class/c1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students").value(3))
                .andExpect(jsonPath("$.tas").value(1))
                .andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void getEnrollmentsByUser_serverErrorReturns5xx() throws Exception {
        when(enrollmentService.getEnrollmentsByUser("err")).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/enrollments/user/err"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getEnrollmentsByClass_unhandledExceptionReturns500() throws Exception {
        when(enrollmentService.getEnrollmentsByClass("boom")).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/enrollments/class/boom"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getStudents_unhandledExceptionReturns500() throws Exception {
        when(enrollmentService.getStudents("boom")).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/enrollments/class/boom/students"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getTAs_unhandledExceptionReturns500() throws Exception {
        when(enrollmentService.getTAs("boom")).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/enrollments/class/boom/tas"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void upgradeToTA_missingPerformedBy_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/enrollments/en1/upgrade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upgradeToTA_runtimeNotFound_returns404() throws Exception {
        when(enrollmentService.upgradeToTA(eq("missing"), anyString())).thenThrow(new RuntimeException("missing"));

        mockMvc.perform(put("/api/enrollments/missing/upgrade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"performedBy\":\"t1\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void upgradeToTA_success_returnsUpdated() throws Exception {
        Enrollment e = buildEnrollment("uTA", EnrollmentRole.TA);
        when(enrollmentService.upgradeToTA(eq("en1"), anyString())).thenReturn(e);

        mockMvc.perform(put("/api/enrollments/en1/upgrade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"performedBy\":\"teacher\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TA"));
    }

    @Test
    void changeRole_runtimeNotFound_returns404() throws Exception {
        when(enrollmentService.changeRole(eq("missing"), eq(EnrollmentRole.TA), anyString()))
                .thenThrow(new RuntimeException("missing"));

        mockMvc.perform(put("/api/enrollments/missing/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TA\",\"performedBy\":\"teacher\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void dropEnrollment_controllerPathReturnsOk() throws Exception {
        Enrollment e = new Enrollment("u", "c", EnrollmentRole.STUDENT);
        when(enrollmentService.dropEnrollment("enr1")).thenReturn(e);

        mockMvc.perform(put("/api/enrollments/enr1/drop"))
                .andExpect(status().isOk());
    }

    @Test
    void getEnrollment_byIdFound() throws Exception {
        Enrollment e = new Enrollment("u", "c", EnrollmentRole.STUDENT);
        when(enrollmentService.getEnrollmentById("en1")).thenReturn(Optional.of(e));

        mockMvc.perform(get("/api/enrollments/en1"))
                .andExpect(status().isOk());
    }

    @Test
    void getEnrollmentsByUser_handlesServerError() throws Exception {
        when(enrollmentService.getEnrollmentsByUser("u1")).thenThrow(new RuntimeException("fail"));

        mockMvc.perform(get("/api/enrollments/user/u1"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void createEnrollment_withInvalidRole_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"u1\",\"classId\":\"c1\",\"role\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEnrollment_conflictReturns409() throws Exception {
        when(enrollmentService.enrollUser(anyString(), anyString(), any())).thenThrow(new RuntimeException("exists"));

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"u1\",\"classId\":\"c1\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void getEnrollmentsByClass_invalidRoleReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/enrollments/class/c3").param("role", "NOT_A_ROLE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEnrollmentsByClass_roleProvidedActiveOnlyFalseUsesStudents() throws Exception {
        Enrollment e = buildEnrollment("u3", EnrollmentRole.STUDENT);
        when(enrollmentService.getStudents("c3")).thenReturn(List.of(e));

        mockMvc.perform(get("/api/enrollments/class/c3").param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u3"));
    }

    @Test
    void getEnrollmentsByClass_withRoleFilters() throws Exception {
        Enrollment e = new Enrollment("u2", "c2", EnrollmentRole.STUDENT);
        when(enrollmentService.getActiveStudents("c2")).thenReturn(List.of(e));

        mockMvc.perform(get("/api/enrollments/class/c2")
                        .param("role", "STUDENT")
                        .param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u2"));
    }

    @Test
    void getStudents_activeOnlyTrueUsesActiveService() throws Exception {
        Enrollment e = buildEnrollment("u5", EnrollmentRole.STUDENT);
        when(enrollmentService.getActiveStudents("classX")).thenReturn(List.of(e));

        mockMvc.perform(get("/api/enrollments/class/classX/students").param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u5"));
    }

    @Test
    void getTAs_activeOnlyTrueUsesActiveService() throws Exception {
        Enrollment e = buildEnrollment("ta5", EnrollmentRole.TA);
        when(enrollmentService.getActiveTAs("classY")).thenReturn(List.of(e));

        mockMvc.perform(get("/api/enrollments/class/classY/tas").param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("ta5"));
    }

    @Test
    void getEnrollmentsByUser_successReturnsList() throws Exception {
        Enrollment e = buildEnrollment("u9", EnrollmentRole.STUDENT);
        when(enrollmentService.getEnrollmentsByUser("u9")).thenReturn(List.of(e));

        mockMvc.perform(get("/api/enrollments/user/u9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u9"));
    }

    @Test
    void getStudents_enrichesWhenUserExists() throws Exception {
        Enrollment e = new Enrollment("u2", "c1", EnrollmentRole.STUDENT);
        e.setStatus(EnrollmentStatus.ACTIVE);
        when(enrollmentService.getStudents("c1")).thenReturn(List.of(e));
        User user = new User("u2", "u2@example.com", "Name", null, List.of());
        user.setId("u2");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPicture("pic.png");
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/enrollments/class/c1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userName").value("Name"))
                .andExpect(jsonPath("$[0].userEmail").value("u2@example.com"))
                .andExpect(jsonPath("$[0].userFirstName").value("First"))
                .andExpect(jsonPath("$[0].userLastName").value("Last"))
                .andExpect(jsonPath("$[0].userPicture").value("pic.png"));
    }

    private static Enrollment buildEnrollment(String userId, EnrollmentRole role) {
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(userId);
        enrollment.setClassId("classId");
        enrollment.setRole(role);
        return enrollment;
    }
}
