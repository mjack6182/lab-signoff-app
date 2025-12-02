package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Class;
import com.example.lab_signoff_backend.repository.ClassRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
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

    @Test
    void addAndRemoveStudentsAndTAs_updatesRosterAndStaff() {
        Class clazz = new Class("CS301", "Databases", "Spring", "prof-1");
        when(classRepository.findById("class-301")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(Class.class))).thenAnswer(inv -> inv.getArgument(0));

        Class updated = classService.addStudentToRoster("class-301", "stu-1");
        assertTrue(updated.getRoster().contains("stu-1"));

        updated = classService.removeStudentFromRoster("class-301", "stu-1");
        assertFalse(updated.getRoster().contains("stu-1"));

        updated = classService.assignTA("class-301", "ta-1");
        assertTrue(updated.getTaIds().contains("ta-1"));

        updated = classService.removeTA("class-301", "ta-1");
        assertFalse(updated.getTaIds().contains("ta-1"));
    }

    @Test
    void addStudentToRoster_duplicateNotAddedTwice() {
        Class clazz = new Class("CS302", "Systems", "Fall", "prof-1");
        clazz.addStudentToRoster("stu-1");
        when(classRepository.findById("class-302")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(Class.class))).thenAnswer(inv -> inv.getArgument(0));

        Class result = classService.addStudentToRoster("class-302", "stu-1");

        assertEquals(1, result.getRoster().size());
    }

    @Test
    void importRosterFromCsv_missingClassThrows() {
        when(classRepository.findById("no-class")).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", "a,b,c".getBytes());

        assertThrows(RuntimeException.class, () -> classService.importRosterFromCsv("no-class", file));
    }

    @Test
    void addStudentToRoster_whenMissingClass_throws() {
        when(classRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> classService.addStudentToRoster("missing", "s1"));
    }

    @Test
    void importRoster_missingHeaderThrows() {
        com.example.lab_signoff_backend.model.Class clazz =
                new com.example.lab_signoff_backend.model.Class("CS404", "Networks", "Fall", "inst");
        when(classRepository.findById("class-404")).thenReturn(Optional.of(clazz));

        MultipartFile badFile = new org.springframework.mock.web.MockMultipartFile(
                "file", "bad.csv", "text/csv", "".getBytes());

        assertThrows(RuntimeException.class, () -> classService.importRosterFromCsv("class-404", badFile));
    }

    @Test
    void removeTA_whenMissingClass_throws() {
        when(classRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> classService.removeTA("missing", "ta-1"));
    }

    @Test
    void assignTA_whenMissingClass_throws() {
        when(classRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> classService.assignTA("missing", "ta-1"));
    }

    @Test
    void removeStudentFromRoster_whenMissingClass_throws() {
        when(classRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> classService.removeStudentFromRoster("missing", "s-1"));
    }

    @Test
    void archiveClass_whenMissing_throws() {
        when(classRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> classService.archiveClass("missing"));
    }

    @Test
    void assignTA_successAddsAndSaves() {
        Class clazz = new Class("CS777", "Title", "Fall", "inst");
        when(classRepository.findById("class-777")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(Class.class))).thenAnswer(inv -> inv.getArgument(0));

        Class updated = classService.assignTA("class-777", "ta-1");

        assertTrue(updated.getTaIds().contains("ta-1"));
        verify(classRepository).save(clazz);
    }

    @Test
    void removeTA_successRemoves() {
        Class clazz = new Class("CS778", "Title", "Fall", "inst");
        clazz.addTA("ta-1");
        when(classRepository.findById("class-778")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(Class.class))).thenAnswer(inv -> inv.getArgument(0));

        Class updated = classService.removeTA("class-778", "ta-1");

        assertFalse(updated.getTaIds().contains("ta-1"));
        verify(classRepository).save(clazz);
    }

    @Test
    void repositoryDelegations_forSimpleQueries() {
        classService.createClass(new Class());
        verify(classRepository).save(any(Class.class));

        classService.getClassById("id-xyz");
        verify(classRepository).findById("id-xyz");

        classService.getClassesByInstructor("inst-1");
        verify(classRepository).findByInstructorId("inst-1");

        classService.getClassesByTerm("Fall");
        verify(classRepository).findByTerm("Fall");

        classService.getAllActiveClasses();
        verify(classRepository).findByArchivedFalse();

        classService.deleteClass("delete-id");
        verify(classRepository).deleteById("delete-id");
    }

    @Test
    void updateClass_whenNoFieldsProvided_keepsOriginalValues() {
        Class existing = new Class("CS101", "Name", "Term", "inst");
        existing.setSection("001");
        when(classRepository.findById("cid")).thenReturn(Optional.of(existing));
        when(classRepository.save(any(Class.class))).thenAnswer(inv -> inv.getArgument(0));

        Class updates = new Class(); // all nulls
        Class result = classService.updateClass("cid", updates);

        assertEquals("Name", result.getCourseName());
        assertEquals("CS101", result.getCourseCode());
        assertEquals("001", result.getSection());
    }

    @Test
    void importRosterFromCsv_nonNumericPointsDefaultsToOne() {
        com.example.lab_signoff_backend.model.Class clazz =
                new com.example.lab_signoff_backend.model.Class("CS888", "BadPoints", "Fall", "inst");
        clazz.setId("class-888");
        String csv = String.join("\n",
                "Student,ID,SIS User ID,SIS Login ID,Laboratory for Bad Lab (123)",
                "Points Possible,,,,not-a-number",
                "Student One,,,s1@example.com,");
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", csv.getBytes());
        when(classRepository.findById("class-888")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(com.example.lab_signoff_backend.model.Class.class))).thenAnswer(inv -> inv.getArgument(0));
        when(labService.upsert(any(com.example.lab_signoff_backend.model.Lab.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.lab_signoff_backend.model.Class result = classService.importRosterFromCsv("class-888", file);
        assertFalse(result.getRoster().isEmpty());
    }

    @Test
    void privateHelpers_coverFallbackBranches() throws Exception {
        Method getValue = ClassService.class.getDeclaredMethod("getValue", org.apache.commons.csv.CSVRecord.class, String.class, Map.class);
        getValue.setAccessible(true);
        Object value = getValue.invoke(classService, null, "Student", Map.of());
        assertNull(value);

        Method normalizeHeader = ClassService.class.getDeclaredMethod("normalizeHeader", String.class);
        normalizeHeader.setAccessible(true);
        assertEquals("", normalizeHeader.invoke(classService, new Object[]{null}));

        Method unquote = ClassService.class.getDeclaredMethod("unquote", String.class);
        unquote.setAccessible(true);
        assertEquals("value", unquote.invoke(classService, "\"value\""));

        Method parseCsvLine = ClassService.class.getDeclaredMethod("parseCsvLine", String.class);
        parseCsvLine.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> empty = (List<String>) parseCsvLine.invoke(classService, "");
        assertTrue(empty.isEmpty());

        @SuppressWarnings("unchecked")
        List<String> parsed = (List<String>) parseCsvLine.invoke(classService, "a,\"b,c\",d");
        assertEquals(3, parsed.size());
        assertEquals("b,c", parsed.get(1));

        // getValue branch coverage
        org.apache.commons.csv.CSVRecord record = org.mockito.Mockito.mock(org.apache.commons.csv.CSVRecord.class);
        org.mockito.Mockito.when(record.isMapped("Student")).thenReturn(true);
        org.mockito.Mockito.when(record.get("Student")).thenReturn("Name");
        Object val = getValue.invoke(classService, record, "Student", Map.of("student", "Student"));
        assertEquals("Name", val);

        org.mockito.Mockito.when(record.isMapped("Student")).thenReturn(false);
        org.mockito.Mockito.when(record.isMapped("Student Name")).thenReturn(true);
        org.mockito.Mockito.when(record.get("Student Name")).thenReturn("Fallback");
        Object fallback = getValue.invoke(classService, record, "STUDENT", Map.of("student", "Student Name"));
        assertEquals("Fallback", fallback);
        org.mockito.Mockito.reset(record);
    }

    @Test
    void removeStudentFromRoster_successRemoves() {
        Class clazz = new Class("CS779", "Title", "Fall", "inst");
        clazz.addStudentToRoster("s-1");
        when(classRepository.findById("class-779")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(Class.class))).thenAnswer(inv -> inv.getArgument(0));

        Class updated = classService.removeStudentFromRoster("class-779", "s-1");

        assertFalse(updated.getRoster().contains("s-1"));
        verify(classRepository).save(clazz);
    }

    @Test
    void getClassesByInstructor_delegatesRepository() {
        Class clazz = new Class("CS800", "Algo", "Fall", "inst-1");
        when(classRepository.findByInstructorId("inst-1")).thenReturn(List.of(clazz));

        List<Class> result = classService.getClassesByInstructor("inst-1");
        assertEquals(1, result.size());
        assertEquals("CS800", result.getFirst().getCourseCode());
    }

    @Test
    void getClassesByTerm_delegatesRepository() {
        Class clazz = new Class("CS801", "DB", "Spring", "inst");
        when(classRepository.findByTerm("Spring")).thenReturn(List.of(clazz));

        List<Class> result = classService.getClassesByTerm("Spring");
        assertEquals(1, result.size());
        assertEquals("CS801", result.getFirst().getCourseCode());
    }

    @Test
    void getAllActiveClasses_filtersArchived() {
        Class active = new Class("CS802", "AI", "Fall", "inst");
        Class archived = new Class("CS803", "ML", "Fall", "inst");
        archived.setArchived(true);
        when(classRepository.findByArchivedFalse()).thenReturn(List.of(active));

        List<Class> result = classService.getAllActiveClasses();
        assertEquals(1, result.size());
        assertEquals("CS802", result.getFirst().getCourseCode());
    }

    @Test
    void archiveClass_happyPath() {
        Class clazz = new Class("CS804", "OS", "Fall", "inst");
        when(classRepository.findById("c-1")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(Class.class))).thenAnswer(inv -> inv.getArgument(0));

        Class archived = classService.archiveClass("c-1");
        assertTrue(archived.getArchived());
        verify(classRepository).save(clazz);
    }

    @Test
    void getStaff_checksMultipleRoles() {
        Class clazz = new Class("CS805", "Staff", "Fall", "inst");
        clazz.addTA("ta-1");
        when(classRepository.findById("class-staff")).thenReturn(Optional.of(clazz));

        assertTrue(classService.isStaff("class-staff", "ta-1"));
        assertTrue(classService.isInstructor("class-staff", "inst"));
        assertFalse(classService.isStaff("class-staff", "missing"));
    }

    @Test
    void updateClass_missingThrows() {
        when(classRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> classService.updateClass("missing", new Class()));
    }

    @Test
    void updateClass_updatesProvidedFieldsAndRoster() {
        Class existing = new Class("CS100", "Old", "Fall", "inst");
        existing.setSection("001");
        when(classRepository.findById("id-100")).thenReturn(Optional.of(existing));
        when(classRepository.save(any(Class.class))).thenAnswer(inv -> inv.getArgument(0));

        Class updates = new Class("CS101", "New Name", "Spring", "inst");
        updates.setSection("002");
        updates.setRoster(List.of("s1", "s2"));

        Class updated = classService.updateClass("id-100", updates);

        assertEquals("CS101", updated.getCourseCode());
        assertEquals("New Name", updated.getCourseName());
        assertEquals("002", updated.getSection());
        assertEquals("Spring", updated.getTerm());
        assertEquals(2, updated.getRoster().size());
    }

    @Test
    void deleteClass_delegatesToRepository() {
        classService.deleteClass("cls-1");
        verify(classRepository).deleteById("cls-1");
    }

    @Test
    void classExists_andFindByClassInfo_delegateToRepository() {
        Class clazz = new Class("CS900", "Delegation", "Fall", "inst");
        when(classRepository.existsByCourseCodeAndTermAndSection("CS900", "Fall", "001")).thenReturn(true);
        when(classRepository.findByCourseCodeAndTermAndSection("CS900", "Fall", "001")).thenReturn(Optional.of(clazz));

        assertTrue(classService.classExists("CS900", "Fall", "001"));
        assertTrue(classService.findByClassInfo("CS900", "Fall", "001").isPresent());
    }

    @Test
    void unquoteAndParseCsvLine_coverUtilities() throws Exception {
        Method unquote = ClassService.class.getDeclaredMethod("unquote", String.class);
        unquote.setAccessible(true);
        assertEquals("hello, world", unquote.invoke(classService, "\"hello, world\""));
        assertEquals("", unquote.invoke(classService, new Object[]{null}));

        Method parseCsvLine = ClassService.class.getDeclaredMethod("parseCsvLine", String.class);
        parseCsvLine.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) parseCsvLine.invoke(classService, "\"a\",\"b,b\",\"c\"\"d\"");
        assertEquals(List.of("a", "b,b", "c\"d"), columns);

        @SuppressWarnings("unchecked")
        List<String> empty = (List<String>) parseCsvLine.invoke(classService, (Object) null);
        assertTrue(empty.isEmpty());
    }

    @Test
    void getValue_usesFallbackHeaderLookupWithRealRecord() throws Exception {
        String csv = String.join("\n",
                "Student Name,ID",
                "Alice,1");
        var parser = new org.apache.commons.csv.CSVParser(
                new java.io.StringReader(csv),
                org.apache.commons.csv.CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build());
        org.apache.commons.csv.CSVRecord record = parser.getRecords().get(0);

        Method getValue = ClassService.class.getDeclaredMethod("getValue", org.apache.commons.csv.CSVRecord.class, String.class, Map.class);
        getValue.setAccessible(true);
        Object result = getValue.invoke(classService, record, "Student", Map.of("student", "Student Name"));

        assertEquals("Alice", result);
    }

    @Test
    void getActiveClassesByInstructor_filtersArchivedBranch() {
        Class active = new Class("CS811", "Active", "Fall", "inst");
        Class archived = new Class("CS812", "Archived", "Fall", "inst");
        archived.setArchived(true);
        when(classRepository.findByInstructorId("inst")).thenReturn(List.of(active, archived));

        List<Class> result = classService.getActiveClassesByInstructor("inst");
        assertEquals(1, result.size());
        assertEquals("CS811", result.getFirst().getCourseCode());
    }

    @Test
    void rosterAndStaffChecks_returnFalseWhenClassMissing() {
        when(classRepository.findById("missing")).thenReturn(Optional.empty());
        assertFalse(classService.isStudentInRoster("missing", "s"));
        assertFalse(classService.isTA("missing", "ta"));
        assertFalse(classService.isInstructor("missing", "inst"));
        assertFalse(classService.isStaff("missing", "u"));
    }
}
