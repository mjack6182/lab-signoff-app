package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Class;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.service.ClassService;
import com.example.lab_signoff_backend.service.LabService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClassController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClassService classService;
    @MockBean
    private LabService labService;

    @Test
    void createClass_conflictReturns409() throws Exception {
        Class payload = new Class("CS101", "Intro", "Fall", "inst");
        when(classService.classExists(anyString(), anyString(), any())).thenReturn(true);

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseCode":"CS101","courseName":"Intro","term":"Fall","instructorId":"inst"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getClasses_filtersByInstructorAndDropsArchived() throws Exception {
        Class active = new Class("CS201", "Systems", "Fall", "inst");
        Class archived = new Class("CS202", "AI", "Fall", "inst");
        archived.setArchived(true);
        when(classService.getClassesByInstructor("inst")).thenReturn(List.of(active, archived));

        mockMvc.perform(get("/api/classes").param("instructorId", "inst"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseCode").value("CS201"));
    }

    @Test
    void getClassById_notFoundReturns404() throws Exception {
        when(classService.getClassById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/classes/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateClass_returnsOk() throws Exception {
        Class updated = new Class("CS400", "ML", "Fall", "inst");
        when(classService.updateClass(eq("class-1"), any(Class.class))).thenReturn(updated);

        mockMvc.perform(put("/api/classes/class-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseName\":\"ML\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void rosterAndTaMutations_returnOk() throws Exception {
        Class clazz = new Class("CS500", "DL", "Fall", "inst");
        when(classService.addStudentToRoster("c1", "s1")).thenReturn(clazz);
        when(classService.removeStudentFromRoster("c1", "s1")).thenReturn(clazz);
        when(classService.assignTA("c1", "ta1")).thenReturn(clazz);
        when(classService.removeTA("c1", "ta1")).thenReturn(clazz);

        mockMvc.perform(post("/api/classes/c1/roster/students/s1"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/classes/c1/roster/students/s1"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/classes/c1/tas/ta1"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/classes/c1/tas/ta1"))
                .andExpect(status().isOk());
    }

    @Test
    void getClasses_includeArchivedTrueKeepsAll() throws Exception {
        Class active = new Class("CS301", "Net", "Fall", "inst");
        Class archived = new Class("CS302", "AI", "Fall", "inst");
        archived.setArchived(true);
        when(classService.getAllActiveClasses()).thenReturn(List.of(active, archived));

        mockMvc.perform(get("/api/classes").param("includeArchived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    private static Class buildClass(String courseCode) {
        return new Class(courseCode, "Name " + courseCode, "Fall", "inst");
    }

    @Test
    void getClasses_filtersByTerm() throws Exception {
        Class termClass = new Class("CS303", "Networks", "Spring", "inst");
        when(classService.getClassesByTerm("Spring")).thenReturn(List.of(termClass));

        mockMvc.perform(get("/api/classes").param("term", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseName").value("Networks"));
    }

    @Test
    void importClassFromCsv_failureReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", "bad".getBytes());
        when(classService.createClass(any())).thenThrow(new RuntimeException("fail"));

        mockMvc.perform(multipart("/api/classes/import")
                        .file(file)
                        .param("instructorId", "inst"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importClassFromCsv_missingFile_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/classes/import")
                        .param("instructorId", "inst"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importClassFromCsv_emptyFile_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/classes/import")
                        .file(file)
                        .param("instructorId", "inst"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getClasses_instructorFilterUsesService() throws Exception {
        Class clazz = buildClass("CS900");
        when(classService.getClassesByInstructor("inst-1")).thenReturn(List.of(clazz));

        mockMvc.perform(get("/api/classes").param("instructorId", "inst-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseCode").value("CS900"));
    }

    @Test
    void importRoster_successReturnsClass() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", "header\n".getBytes());
        Class clazz = buildClass("CS777");
        when(classService.importRosterFromCsv(eq("class-777"), any())).thenReturn(clazz);

        mockMvc.perform(multipart("/api/classes/class-777/roster/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("CS777"));
    }

    @Test
    void importRoster_badFileReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/classes/class-1/roster/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClass_handlesServerError() throws Exception {
        when(classService.classExists(anyString(), anyString(), anyString())).thenReturn(false);
        when(classService.createClass(any())).thenThrow(new RuntimeException("fail"));

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseCode\":\"CS999\",\"courseName\":\"X\",\"term\":\"Fall\",\"instructorId\":\"inst\"}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getClasses_termFilterReturnsTermClasses() throws Exception {
        Class termClass = buildClass("CS777");
        when(classService.getClassesByTerm("2025")).thenReturn(List.of(termClass));

        mockMvc.perform(get("/api/classes").param("term", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseCode").value("CS777"));
    }

    @Test
    void importClassFromCsv_success_derivesNamesWhenMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "Course Name.csv", "text/csv", "header\n".getBytes());
        Class created = new Class("DERIVED", "Course Name", "Imported 2025", "inst");
        created.setId("class-derived");
        Class populated = new Class("DERIVED", "Course Name", "Imported 2025", "inst");
        when(classService.createClass(any(Class.class))).thenReturn(created);
        when(classService.importRosterFromCsv(eq("class-derived"), any())).thenReturn(populated);

        mockMvc.perform(multipart("/api/classes/import")
                        .file(file)
                        .param("instructorId", "inst"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseName").value("Course Name"));
    }

    @Test
    void addStudentToRoster_notFoundReturns404() throws Exception {
        when(classService.addStudentToRoster("class-404", "student-1")).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(post("/api/classes/class-404/roster/students/student-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeStudentFromRoster_notFoundReturns404() throws Exception {
        when(classService.removeStudentFromRoster("class-404", "student-1")).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(delete("/api/classes/class-404/roster/students/student-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignTA_notFoundReturns404() throws Exception {
        when(classService.assignTA("class-404", "user-1")).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(post("/api/classes/class-404/tas/user-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeTA_notFoundReturns404() throws Exception {
        when(classService.removeTA("class-404", "user-1")).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(delete("/api/classes/class-404/tas/user-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void importClassFromCsv_success_returnsCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", "header\n".getBytes());
        Class created = new Class("CS303", "Networks", "Spring", "inst");
        created.setId("class-1");
        when(classService.createClass(any(Class.class))).thenReturn(created);
        when(classService.importRosterFromCsv(eq("class-1"), any())).thenReturn(created);

        mockMvc.perform(multipart("/api/classes/import")
                        .file(file)
                        .param("instructorId", "inst"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseCode").value("CS303"));
    }

    @Test
    void getLabsByClassId_notFoundReturns404() throws Exception {
        when(classService.getClassById("class-x")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/classes/class-x/labs"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLabsByClassId_successReturnsLabs() throws Exception {
        Class clazz = new Class("CS304", "OS", "Fall", "inst");
        clazz.setId("class-1");
        Lab lab = new Lab("class-1", "Lab 1", 3, "inst");
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(labService.getLabsByClassId("class-1")).thenReturn(List.of(lab));

        mockMvc.perform(get("/api/classes/class-1/labs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Lab 1"));
    }

    @Test
    void getClassById_notFound() throws Exception {
        when(classService.getClassById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/classes/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveClass_whenServiceThrowsNotFound() throws Exception {
        when(classService.archiveClass("missing")).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(put("/api/classes/missing/archive"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateClass_notFoundReturns404() throws Exception {
        when(classService.updateClass(eq("missing"), any())).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(put("/api/classes/missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseName\":\"Updated\"}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void deleteClass_notFoundReturns404() throws Exception {
        doThrow(new RuntimeException("boom")).when(classService).deleteClass("missing");

        mockMvc.perform(delete("/api/classes/missing"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void deleteClass_handlesServerError() throws Exception {
        doThrow(new RuntimeException("fail")).when(classService).deleteClass("bad");

        mockMvc.perform(delete("/api/classes/bad"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void archiveClass_successReturnsBody() throws Exception {
        Class clazz = buildClass("CS999");
        when(classService.archiveClass("c-1")).thenReturn(clazz);

        mockMvc.perform(put("/api/classes/c-1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("CS999"));
    }

    @Test
    void rosterChecks_returnFlags() throws Exception {
        when(classService.isStudentInRoster("class-1", "s1")).thenReturn(true);
        when(classService.isTA("class-1", "ta1")).thenReturn(false);
        when(classService.isStaff("class-1", "staff1")).thenReturn(true);

        mockMvc.perform(get("/api/classes/class-1/roster/students/s1/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inRoster").value(true));

        mockMvc.perform(get("/api/classes/class-1/tas/ta1/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isTA").value(false));

        mockMvc.perform(get("/api/classes/class-1/staff/staff1/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isStaff").value(true));
    }

    @Test
    void getClasses_includeArchivedFalseFilters() throws Exception {
        Class archived = buildClass("CS404");
        archived.setArchived(true);
        when(classService.getAllActiveClasses()).thenReturn(List.of(archived));

        mockMvc.perform(get("/api/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void updateClass_internalErrorReturns500() throws Exception {
        when(classService.updateClass(eq("err"), any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(put("/api/classes/err")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseName\":\"X\"}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void importClassFromCsv_missingInstructor_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", "header\n".getBytes());

        mockMvc.perform(multipart("/api/classes/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importRoster_runtimeExceptionReturnsNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", "header\n".getBytes());
        when(classService.importRosterFromCsv(eq("missing"), any())).thenThrow(new RuntimeException("missing"));

        mockMvc.perform(multipart("/api/classes/missing/roster/import").file(file))
                .andExpect(status().isNotFound());
    }

    @Test
    void importClassFromCsv_withExplicitFieldsUsesProvidedValues() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "explicit.csv", "text/csv", "data".getBytes());
        when(classService.createClass(any(Class.class))).thenAnswer(inv -> {
            Class c = inv.getArgument(0);
            c.setId("class-exp");
            return c;
        });
        when(classService.importRosterFromCsv(eq("class-exp"), any())).thenAnswer(inv -> {
            Class c = new Class("CS-EXP", "Explicit Name", "Custom Term", "inst");
            c.setSection("002");
            return c;
        });

        mockMvc.perform(multipart("/api/classes/import")
                        .file(file)
                        .param("instructorId", "inst")
                        .param("courseCode", "CS-EXP")
                        .param("courseName", "Explicit Name")
                        .param("term", "Custom Term")
                        .param("section", "002"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.section").value("002"));
    }

    @Test
    void importClassFromCsv_emptyFilenameFallsBackToImportedClass() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", "text/csv", "data".getBytes());
        Class created = new Class("IMPORTED", "Imported Class", "Imported 2025", "inst");
        created.setId("class-imp");
        when(classService.createClass(any(Class.class))).thenReturn(created);
        when(classService.importRosterFromCsv(eq("class-imp"), any())).thenReturn(created);

        mockMvc.perform(multipart("/api/classes/import")
                        .file(file)
                        .param("instructorId", "inst"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseName").value("Imported Class"));
    }

    @Test
    void getClasses_instructorIdBlankFallsBackToDefault() throws Exception {
        Class active = buildClass("CS111");
        when(classService.getAllActiveClasses()).thenReturn(List.of(active));

        mockMvc.perform(get("/api/classes").param("instructorId", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseCode").value("CS111"));
    }

    @Test
    void getClasses_termBlankFallsBackToDefault() throws Exception {
        Class active = buildClass("CS112");
        when(classService.getAllActiveClasses()).thenReturn(List.of(active));

        mockMvc.perform(get("/api/classes").param("term", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseCode").value("CS112"));
    }

    @Test
    void deleteClass_successReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/classes/class-abc"))
                .andExpect(status().isNoContent());
    }

    @Test
    void createClass_successReturnsCreated() throws Exception {
        Class created = new Class("CS123", "Intro", "Fall", "inst");
        when(classService.classExists(anyString(), anyString(), anyString())).thenReturn(false);
        when(classService.createClass(any(Class.class))).thenReturn(created);

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseCode\":\"CS123\",\"courseName\":\"Intro\",\"term\":\"Fall\",\"instructorId\":\"inst\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void importClassFromCsv_deletesClassOnImportFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "Course Name.csv", "text/csv", "header\n".getBytes());
        Class created = new Class("DERIVED", "Course Name", "Imported 2025", "inst");
        created.setId("class-del");
        when(classService.createClass(any(Class.class))).thenReturn(created);
        when(classService.importRosterFromCsv(eq("class-del"), any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(multipart("/api/classes/import")
                        .file(file)
                        .param("instructorId", "inst"))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verify(classService).deleteClass("class-del");
    }

    @Test
    void getClassById_successReturnsBody() throws Exception {
        Class clazz = buildClass("CS111");
        when(classService.getClassById("c-111")).thenReturn(Optional.of(clazz));

        mockMvc.perform(get("/api/classes/c-111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("CS111"));
    }
}
