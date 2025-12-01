package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Class;
import com.example.lab_signoff_backend.repository.ClassRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock
    private ClassRepository classRepository;

    @Mock
    private LabService labService;

    @InjectMocks
    private ClassService classService;

    @Test
    void updateClass_updatesFieldsAndSaves() {
        Class existing = new Class("CS101", "Intro CS", "Fall", "instructor-1");
        existing.setSection("001");
        when(classRepository.findById("id-1")).thenReturn(Optional.of(existing));
        when(classRepository.save(any(Class.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Class updates = new Class("CS101", "Intro CS Updated", "Fall", "instructor-1");
        updates.setSection("002");

        Class result = classService.updateClass("id-1", updates);

        assertEquals("Intro CS Updated", result.getCourseName());
        assertEquals("002", result.getSection());
        verify(classRepository).save(existing);
    }

    @Test
    void updateClass_whenMissing_throws() {
        when(classRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> classService.updateClass("missing", new Class()));
    }

    @Test
    void archiveClass_setsArchived() {
        Class existing = new Class("CS102", "Data Structures", "Fall", "instructor-2");
        when(classRepository.findById("id-2")).thenReturn(Optional.of(existing));
        when(classRepository.save(any(Class.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Class result = classService.archiveClass("id-2");

        assertTrue(result.getArchived());
    }

    @Test
    void rosterAndStaffChecks_reflectUnderlyingClass() {
        Class clazz = new Class("CS103", "Algorithms", "Spring", "prof");
        clazz.addStudentToRoster("student-1");
        clazz.addTA("ta-1");

        when(classRepository.findById("class-1")).thenReturn(Optional.of(clazz));

        assertTrue(classService.isStudentInRoster("class-1", "student-1"));
        assertTrue(classService.isTA("class-1", "ta-1"));
        assertTrue(classService.isInstructor("class-1", "prof"));
        assertTrue(classService.isStaff("class-1", "ta-1"));
    }

    @Test
    void getActiveClassesByInstructor_filtersArchived() {
        Class active = new Class("CS201", "Systems", "Fall", "inst");
        Class archived = new Class("CS202", "AI", "Fall", "inst");
        archived.setArchived(true);
        when(classRepository.findByInstructorId("inst")).thenReturn(List.of(active, archived));

        List<Class> result = classService.getActiveClassesByInstructor("inst");

        assertEquals(1, result.size());
        assertEquals("CS201", result.getFirst().getCourseCode());
    }
}
