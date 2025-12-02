package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Class;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClassController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClassControllerImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClassService classService;
    @MockBean
    private LabService labService;

    @Test
    void importClassFromCsv_successfulPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv",
                ("Student,ID,Laboratory for Module 01 (123)\n" +
                        "Points Possible,,2\n" +
                        "Student One,1,\n").getBytes());
        Class clazz = new Class("CS111", "Test", "Fall", "inst");
        clazz.setId("c-1");
        when(classService.createClass(any(Class.class))).thenReturn(clazz);
        when(classService.importRosterFromCsv("c-1", file)).thenReturn(clazz);

        mockMvc.perform(multipart("/api/classes/import")
                        .file(file)
                        .param("instructorId", "inst"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseCode").value("CS111"));
    }
}
