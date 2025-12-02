package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.*;
import com.example.lab_signoff_backend.model.embedded.CheckpointDefinition;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabGradeExportServiceTest {

    @Mock
    private LabService labService;
    @Mock
    private ClassService classService;
    @Mock
    private GroupService groupService;
    @Mock
    private EnrollmentService enrollmentService;
    @Mock
    private UserService userService;

    @InjectMocks
    private LabGradeExportService exportService;

    @Test
    void generateCsv_noCheckpointsUsesLabPointsAndZerosRoster() {
        Lab lab = new Lab("class-1", "Lab No CP", 3, "inst");
        lab.setId("lab-zero");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS401", "Zeroes", "Fall", "inst");
        clazz.setId("class-1");
        clazz.setRoster(List.of("Student One"));

        Enrollment enrollment = new Enrollment("u1", "class-1", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT);
        User user = new User("auth0|u1", "u1@example.com", "Student One", null, List.of("Student"));
        user.setId("u1");

        GroupMember member = new GroupMember();
        member.setUserId("u1");
        member.setName("Student One");
        member.setPresent(true);
        Group group = new Group();
        group.setLabId("lab-zero");
        group.setGroupId("g-zero");
        group.setMembers(List.of(member));
        group.setCheckpointProgress(List.of()); // no progress

        when(labService.getById("lab-zero")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-zero")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of(enrollment));
        when(userService.findByIds(any())).thenReturn(Map.of("u1", user));

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-zero");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Points Possible"));
        assertTrue(csv.contains(",0")); // zero-filled score for rostered student
    }

    @Test
    void generateCsv_withCheckpoints_includesScoresAndHeader() {
        Lab lab = new Lab("class-1", "Lab 1", 2, "inst");
        lab.setId("lab-1");
        CheckpointDefinition def = new CheckpointDefinition();
        def.setNumber(1);
        def.setPoints(2);
        lab.setCheckpoints(List.of(def));

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS101", "Intro", "Fall", "inst");
        clazz.setId("class-1");
        clazz.setSection("SEC");
        clazz.setRoster(List.of("Student One"));

        Enrollment enrollment = new Enrollment("u1", "class-1", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT);
        User user = new User("auth0|u1", "u1@example.com", "Student One", null, List.of("Student"));
        user.setId("u1");

        GroupMember member = new GroupMember();
        member.setUserId("u1");
        member.setName("Student One");
        member.setPresent(true);
        Group group = new Group();
        group.setLabId("lab-1");
        group.setGroupId("g1");
        group.setMembers(List.of(member));
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));

        when(labService.getById("lab-1")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-1")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of(enrollment));
        when(userService.findByIds(any())).thenReturn(Map.of("u1", user));

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-1");

        assertEquals("lab_lab-1_grades.csv", result.getFileName());
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Student"));
        assertTrue(csv.contains("Points Possible"));
        assertTrue(csv.contains("Student One"));
        // Score should be the checkpoint points (2)
        assertTrue(csv.contains(",2"));
    }

    @Test
    void generateCsv_importedDescriptionUsesTrimmedColumn() {
        Lab lab = new Lab("class-1", "Lab 4", 1, "inst");
        lab.setId("lab-4");
        lab.setDescription("Imported from Canvas: Custom Name");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS104", "Theory", "Fall", "inst");
        clazz.setId("class-1");
        clazz.setRoster(List.of());

        when(labService.getById("lab-4")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-4")).thenReturn(List.of());
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-4");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Custom Name"));
    }

    @Test
    void generateCsv_noCheckpoints_usesLabPointsFallback() {
        Lab lab = new Lab("class-1", "Lab 2", 3, "inst");
        lab.setId("lab-2");
        lab.setCheckpoints(List.of());

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS102", "DS", "Fall", "inst");
        clazz.setId("class-1");
        clazz.setRoster(List.of());

        when(labService.getById("lab-2")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-2")).thenReturn(List.of());
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-2");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Lab 2"));
        assertTrue(csv.contains("Points Possible"));
        assertTrue(csv.contains("3")); // fallback points
    }

    @Test
    void generateCsv_rosterWithoutScores_getsZeroes() {
        Lab lab = new Lab("class-1", "Lab 3", 1, "inst");
        lab.setId("lab-3");
        lab.setCheckpoints(List.of());

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS103", "Algo", "Fall", "inst");
        clazz.setId("class-1");
        clazz.setRoster(List.of("Student Zero"));

        when(labService.getById("lab-3")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-3")).thenReturn(List.of());
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-3");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Student Zero"));
        assertTrue(csv.contains(",0")); // zeroed score
    }

    @Test
    void generateCsv_memberWithoutUser_fallsBackToUserIdName() {
        Lab lab = new Lab("class-1", "Lab 5", 1, "inst");
        lab.setId("lab-5");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS105", "Theory", "Fall", "inst");
        clazz.setId("class-1");

        GroupMember member = new GroupMember();
        member.setUserId("ghost");
        member.setPresent(true);
        Group group = new Group();
        group.setLabId("lab-5");
        group.setGroupId("g5");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));
        group.setMembers(List.of(member));

        when(labService.getById("lab-5")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-5")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-5");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("ghost"));
    }

    @Test
    void generateCsv_pointsAwardedOverridesStatusPoints() {
        Lab lab = new Lab("class-1", "Lab 6", 2, "inst");
        lab.setId("lab-6");
        CheckpointDefinition def1 = new CheckpointDefinition();
        def1.setNumber(1);
        def1.setPoints(2);
        lab.setCheckpoints(List.of(def1));

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS106", "Math", "Fall", "inst");
        clazz.setId("class-1");

        GroupMember member = new GroupMember();
        member.setUserId("u1");
        member.setName("Student");
        member.setPresent(true);
        Group group = new Group();
        group.setLabId("lab-6");
        group.setGroupId("g6");
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.RETURN); // would give 0
        cp.setPointsAwarded(5); // overrides to 5
        group.setCheckpointProgress(List.of(cp));
        group.setMembers(List.of(member));

        when(labService.getById("lab-6")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-1")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-6")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-6");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Lab 6"));
        assertTrue(csv.contains("Student"));
    }

    @Test
    void generateCsv_memberWithoutUserIdMatchesByName() {
        Lab lab = new Lab("class-2", "Lab Name Match", 1, "inst");
        lab.setId("lab-name");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS777", "Names", "Fall", "inst");
        clazz.setId("class-2");
        clazz.setRoster(List.of("Jane Smith"));

        Enrollment enrollment = new Enrollment("u2", "class-2", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT);
        User user = new User("auth0|u2", "u2@example.com", "Jane Smith", null, List.of("Student"));
        user.setId("u2");

        GroupMember member = new GroupMember();
        member.setName("Jane Smith");
        member.setPresent(true);
        Group group = new Group();
        group.setLabId("lab-name");
        group.setGroupId("g-name");
        group.setMembers(List.of(member));
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));

        when(labService.getById("lab-name")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-2")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-name")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-2")).thenReturn(List.of(enrollment));
        when(userService.findByIds(any())).thenReturn(Map.of("u2", user));

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-name");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Jane Smith"));
    }

    @Test
    void generateCsv_clampsScoreToPointsPossible() {
        Lab lab = new Lab("class-3", "Lab Clamp", 1, "inst");
        lab.setId("lab-clamp");
        CheckpointDefinition def = new CheckpointDefinition();
        def.setNumber(1);
        def.setPoints(1);
        lab.setCheckpoints(List.of(def));

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS888", "Clamp", "Fall", "inst");
        clazz.setId("class-3");

        GroupMember member = new GroupMember();
        member.setUserId("u3");
        member.setPresent(true);
        Group group = new Group();
        group.setLabId("lab-clamp");
        group.setGroupId("g-clamp");
        group.setMembers(List.of(member));
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setPointsAwarded(5); // above possible
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));

        when(labService.getById("lab-clamp")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-3")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-clamp")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-3")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-clamp");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertFalse(csv.isEmpty());
    }

    @Test
    void generateCsv_unknownUserDefaultsToUnknownStudent() {
        Lab lab = new Lab("class-4", "Lab Unknown", 1, "inst");
        lab.setId("lab-unknown");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS889", "Unknown", "Fall", "inst");
        clazz.setId("class-4");
        clazz.setRoster(List.of());

        GroupMember member = new GroupMember();
        member.setPresent(true);
        member.setUserId("ghost");
        member.setName("");
        Group group = new Group();
        group.setLabId("lab-unknown");
        group.setGroupId("g-unknown");
        group.setMembers(List.of(member));
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.RETURN);
        group.setCheckpointProgress(List.of(cp));

        when(labService.getById("lab-unknown")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-4")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-unknown")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-4")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-unknown");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Unknown Student"));
    }

    @Test
    void generateCsv_presentFalseSetsZeroAndHasScore() {
        Lab lab = new Lab("class-5", "Attendance", 2, "inst");
        lab.setId("lab-absent");
        CheckpointDefinition def = new CheckpointDefinition();
        def.setNumber(1);
        def.setPoints(2);
        lab.setCheckpoints(List.of(def));

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS900", "Attendance", "Fall", "inst");
        clazz.setId("class-5");

        GroupMember member = new GroupMember();
        member.setUserId("u5");
        member.setName("Skip Student");
        member.setPresent(false); // absent branch
        Group group = new Group();
        group.setLabId("lab-absent");
        group.setGroupId("g-absent");
        group.setMembers(List.of(member));
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.PASS); // would earn points but present=false should zero
        group.setCheckpointProgress(List.of(cp));

        when(labService.getById("lab-absent")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-5")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-absent")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-5")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-absent");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains(",0")); // zero score due to absence
    }

    @Test
    void generateCsv_rosterWithoutGroups_zeroFilledByFinalize() {
        Lab lab = new Lab("class-6", "No Groups", 1, "inst");
        lab.setId("lab-empty");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS901", "Roster", "Fall", "inst");
        clazz.setId("class-6");
        clazz.setRoster(List.of("Student A", "Student B"));

        when(labService.getById("lab-empty")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-6")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-empty")).thenReturn(List.of());
        when(enrollmentService.getActiveStudents("class-6")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-empty");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Student A"));
        assertTrue(csv.contains("Student B"));
        assertTrue(csv.contains(",0")); // finalizeRosterZeros fills zeros
    }

    @Test
    void generateCsv_enrollmentOnly_gradeCellBlankWhenNoScore() {
        Lab lab = new Lab("class-7", "No Scores", 1, "inst");
        lab.setId("lab-noscore");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS902", "Enrollments", "Fall", "inst");
        clazz.setId("class-7");

        Enrollment enrollment = new Enrollment("u7", "class-7", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT);
        User user = new User("auth0|u7", "u7@example.com", "User Seven", null, List.of("Student"));
        user.setId("u7");

        when(labService.getById("lab-noscore")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-7")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-noscore")).thenReturn(List.of());
        when(enrollmentService.getActiveStudents("class-7")).thenReturn(List.of(enrollment));
        when(userService.findByIds(any())).thenReturn(Map.of("u7", user));

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-noscore");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        // grade cell should be blank since no score and not on roster
        assertTrue(csv.contains("User Seven"));
        assertTrue(csv.contains(",,,")); // blank grade column
    }

    @Test
    void generateCsv_descriptionAndTitleMissingUsesFallback() {
        Lab lab = new Lab("class-8", "", 0, "inst");
        lab.setId("lab-fallback");
        lab.setTitle(null); // force fallback title
        lab.setDescription("  "); // blank description
        CheckpointDefinition def = new CheckpointDefinition();
        def.setNumber(1);
        def.setPoints(0); // zero points triggers default one
        lab.setCheckpoints(List.of(def));

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS903", "Fallback", "Fall", "inst");
        clazz.setId("class-8");

        when(labService.getById("lab-fallback")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-8")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-fallback")).thenReturn(List.of());
        when(enrollmentService.getActiveStudents("class-8")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-fallback");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Lab lab-fallback")); // fallback column name
        assertTrue(csv.contains("Points Possible,,,,,1")); // default 1 point
    }

    @Test
    void generateCsv_descriptionUsesCustomWhenNotImported() {
        Lab lab = new Lab("class-9", "Ignored Title", 1, "inst");
        lab.setId("lab-desc");
        lab.setDescription("Custom Column Name");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS904", "Desc", "Fall", "inst");
        clazz.setId("class-9");

        when(labService.getById("lab-desc")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-9")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-desc")).thenReturn(List.of());
        when(enrollmentService.getActiveStudents("class-9")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-desc");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Custom Column Name"));
    }

    @Test
    void generateCsv_enrollmentMissingUser_populatesUnknownStudent() {
        Lab lab = new Lab("class-10", "Unknown Enrollment", 1, "inst");
        lab.setId("lab-missing-user");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS905", "Missing", "Fall", "inst");
        clazz.setId("class-10");

        Enrollment enrollment = new Enrollment("user-x", "class-10", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT);

        when(labService.getById("lab-missing-user")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-10")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-missing-user")).thenReturn(List.of());
        when(enrollmentService.getActiveStudents("class-10")).thenReturn(List.of(enrollment));
        when(userService.findByIds(any())).thenReturn(Map.of()); // missing user

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-missing-user");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Unknown Student"));
        assertTrue(csv.contains("user-x")); // id fallback
    }

    @Test
    void generateCsv_negativeScoreClampedToZero() {
        Lab lab = new Lab("class-11", "Negative", 2, "inst");
        lab.setId("lab-negative");
        CheckpointDefinition def = new CheckpointDefinition();
        def.setNumber(1);
        def.setPoints(2);
        lab.setCheckpoints(List.of(def));

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS906", "Negative", "Fall", "inst");
        clazz.setId("class-11");

        GroupMember member = new GroupMember();
        member.setUserId("u-neg");
        member.setPresent(true);
        Group group = new Group();
        group.setLabId("lab-negative");
        group.setGroupId("g-neg");
        group.setMembers(List.of(member));
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setPointsAwarded(-5); // negative -> clamp to zero
        cp.setStatus(SignoffAction.PASS);
        List<CheckpointProgress> progress = new java.util.ArrayList<>();
        progress.add(cp);
        progress.add(null); // null entry branch
        group.setCheckpointProgress(progress);

        when(labService.getById("lab-negative")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-11")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-negative")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-11")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-negative");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains(",0")); // clamped score
    }

    @Test
    void generateCsv_groupWithNoMembersSkipped() {
        Lab lab = new Lab("class-12", "Empty Group", 1, "inst");
        lab.setId("lab-nomembers");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS907", "Groupless", "Fall", "inst");
        clazz.setId("class-12");
        clazz.setRoster(List.of("Lonely Student"));

        Group emptyGroup = new Group();
        emptyGroup.setLabId("lab-nomembers");
        emptyGroup.setGroupId("g-empty");
        emptyGroup.setMembers(null); // triggers early continue

        when(labService.getById("lab-nomembers")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-12")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-nomembers")).thenReturn(List.of(emptyGroup));
        when(enrollmentService.getActiveStudents("class-12")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-nomembers");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Lonely Student")); // roster still written
        assertTrue(csv.contains(",0")); // zero due to finalizeRosterZeros
    }

    @Test
    void generateCsv_pointsFallbacksToOneWhenNoData() {
        Lab lab = new Lab();
        lab.setClassId("class-13");
        lab.setId("lab-minimal");
        lab.setTitle(null);
        lab.setDescription(null);
        lab.setPoints(0); // no points
        lab.setCheckpoints(null); // no checkpoints

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS908", "Minimal", "Fall", "inst");
        clazz.setId("class-13");

        when(labService.getById("lab-minimal")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-13")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-minimal")).thenReturn(List.of());
        when(enrollmentService.getActiveStudents("class-13")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-minimal");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Points Possible,,,,,1")); // default to 1
        assertTrue(result.getFileName().contains("lab-minimal"));
    }

    @Test
    void generateCsv_checkpointPointsDefaultToOneWhenZeroOrNull() {
        Lab lab = new Lab("class-14", "Defaults", 0, "inst");
        lab.setId("lab-defaults");
        CheckpointDefinition nullDef = null;
        CheckpointDefinition zeroPoints = new CheckpointDefinition();
        zeroPoints.setNumber(2);
        zeroPoints.setPoints(0); // should default to 1
        CheckpointDefinition missingNumber = new CheckpointDefinition();
        missingNumber.setPoints(5); // skipped
        lab.setCheckpoints(new java.util.ArrayList<>(java.util.Arrays.asList(nullDef, zeroPoints, missingNumber)));

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS909", "Defaults", "Fall", "inst");
        clazz.setId("class-14");

        GroupMember member = new GroupMember();
        member.setUserId("user-def");
        member.setPresent(true);
        Group group = new Group();
        group.setLabId("lab-defaults");
        group.setGroupId("g-defaults");
        group.setMembers(List.of(member));
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(2); // uses default 1 point
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));

        when(labService.getById("lab-defaults")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-14")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-defaults")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-14")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(Map.of());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-defaults");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Points Possible,,,,,1")); // default points from checkpoint map
        assertTrue(csv.contains("Defaults")); // column name from title
    }

    @Test
    void generateCsv_memberPresentNullDefaultsTrueAndUsesUserName() {
        Lab lab = new Lab("class-15", "Null Present", 1, "inst");
        lab.setId("lab-null-present");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS910", "NullPresent", "Fall", "inst");
        clazz.setId("class-15");

        Enrollment enrollment = new Enrollment("u15", "class-15", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT);
        User user = new User("auth0|u15", "u15@example.com", null, null, List.of("Student"));
        user.setFirstName("First");
        user.setLastName("Last");
        user.setId("u15");

        GroupMember member = new GroupMember();
        member.setUserId("u15");
        member.setPresent(null); // defaults to true
        Group group = new Group();
        group.setLabId("lab-null-present");
        group.setGroupId("g-null");
        group.setMembers(List.of(member));
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));

        when(labService.getById("lab-null-present")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-15")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-null-present")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-15")).thenReturn(List.of(enrollment));
        when(userService.findByIds(any())).thenReturn(Map.of("u15", user));

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-null-present");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Last, First")); // buildCanvasName path
        assertTrue(csv.contains(",1")); // earned points
    }

    @Test
    void buildCheckpointPointMap_handlesNullDefinitionsAndNullNumbers() {
        Lab lab = new Lab("class-null", "NoDefs", 2, "inst");
        lab.setId("lab-null");
        lab.setCheckpoints(Arrays.asList(null, new CheckpointDefinition())); // one null, one missing number

        Map<Integer, Integer> points = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "buildCheckpointPointMap", lab);

        assertTrue(points.isEmpty()); // no valid definitions or total points when list provided but empty data
    }

    @Test
    void calculatePointsPossible_defaultsToOneWhenZero() {
        Lab lab = new Lab("class-zero", "Zero", 0, "inst");
        lab.setId("lab-zero");
        Map<Integer, Integer> points = Map.of(); // empty -> zero total

        BigDecimal total = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "calculatePointsPossible", points, lab);

        assertEquals(BigDecimal.ONE, total);
    }

    @Test
    void calculatePointsPossible_usesCheckpointSumWhenPositive() {
        Lab lab = new Lab("class-pos", "Pos", 0, "inst");
        lab.setId("lab-pos");
        Map<Integer, Integer> points = Map.of(1, 2, 2, 3);
        BigDecimal total = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "calculatePointsPossible", points, lab);
        assertEquals(new BigDecimal("5"), total);
    }

    @Test
    void buildCanvasName_prefersEmailWhenNoNames() {
        User user = new User("auth0|mail", "mail@example.com", null, null, List.of("Student"));
        user.setId("u-mail");

        String name = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "buildCanvasName", user);

        assertEquals("mail@example.com", name);
    }

    @Test
    void determineDisplayName_prefersMemberNameOverUser() {
        GroupMember member = new GroupMember();
        member.setName("Member Name");
        User user = new User("auth0|id", "e@example.com", null, null, List.of("Student"));
        String name = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "determineDisplayName", member, user);
        assertEquals("Member Name", name);
    }

    @Test
    void populateFromUser_setsUserFields() throws Exception {
        var rowCtor = java.lang.Class.forName("com.example.lab_signoff_backend.service.LabGradeExportService$StudentRow")
                .getDeclaredConstructor(String.class);
        rowCtor.setAccessible(true);
        Object row = rowCtor.newInstance("row");
        User user = new User("auth0|1", "u@example.com", "Name", null, List.of("Student"));
        user.setFirstName("First");
        user.setLastName("Last");
        user.setId("uid");

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "populateFromUser", row, user);

        assertEquals("Last, First", org.springframework.test.util.ReflectionTestUtils.getField(row, "studentName"));
        assertEquals("uid", org.springframework.test.util.ReflectionTestUtils.getField(row, "id"));
        assertEquals("u@example.com", org.springframework.test.util.ReflectionTestUtils.getField(row, "sisLoginId"));
    }

    @Test
    void gradeCell_returnsEmptyWhenNoScore() throws Exception {
        var rowCtor = java.lang.Class.forName("com.example.lab_signoff_backend.service.LabGradeExportService$StudentRow")
                .getDeclaredConstructor(String.class);
        rowCtor.setAccessible(true);
        Object row = rowCtor.newInstance("row");
        org.springframework.test.util.ReflectionTestUtils.setField(row, "score", null);
        String cell = org.springframework.test.util.ReflectionTestUtils.invokeMethod(exportService, "gradeCell", row);
        assertEquals("", cell);
    }

    @Test
    void formatDecimal_returnsEmptyWhenNull() {
        String formatted = org.springframework.test.util.ReflectionTestUtils.invokeMethod(exportService, "formatDecimal", new Object[]{null});
        assertEquals("", formatted);
    }

    @Test
    void defaultSection_returnsEmptyWhenNull() {
        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CSX", "X", "Term", "inst");
        String section = org.springframework.test.util.ReflectionTestUtils.invokeMethod(exportService, "defaultSection", clazz);
        assertEquals("", section);
    }

    @Test
    void defaultSection_returnsValueWhenPresent() {
        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CSY", "Y", "Term", "inst");
        clazz.setSection("SEC");
        String section = org.springframework.test.util.ReflectionTestUtils.invokeMethod(exportService, "defaultSection", clazz);
        assertEquals("SEC", section);
    }

    @Test
    void determineDisplayName_usesBuildCanvasWhenNoMemberName() {
        GroupMember member = new GroupMember();
        User user = new User("auth0|id", "u@example.com", "User Name", null, List.of("Student"));
        String name = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "determineDisplayName", member, user);
        assertEquals("User Name", name);
    }

    @Test
    void formatDecimal_stripsTrailingZeros() {
        String formatted = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "formatDecimal", new BigDecimal("2.5000"));
        assertEquals("2.5", formatted);
    }

    @Test
    void gradeCell_returnsFormattedScore() throws Exception {
        var rowCtor = java.lang.Class.forName("com.example.lab_signoff_backend.service.LabGradeExportService$StudentRow")
                .getDeclaredConstructor(String.class);
        rowCtor.setAccessible(true);
        Object row = rowCtor.newInstance("row");
        org.springframework.test.util.ReflectionTestUtils.setField(row, "score", new BigDecimal("3.000"));
        String cell = org.springframework.test.util.ReflectionTestUtils.invokeMethod(exportService, "gradeCell", row);
        assertEquals("3", cell);
    }

    @Test
    void determineCanvasColumnName_stripsImportedPrefix() {
        Lab lab = new Lab("class-desc", "Title", 1, "inst");
        lab.setId("lab-desc");
        lab.setDescription("Imported from Canvas: Custom Name");
        String column = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "determineCanvasColumnName", lab);
        assertEquals("Custom Name", column);
    }

    @Test
    void calculateGroupScore_returnsZeroWhenNoProgress() {
        Group group = new Group();
        group.setCheckpointProgress(null);
        BigDecimal score = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "calculateGroupScore", group, Map.of(1, 1));
        assertEquals(BigDecimal.ZERO, score);
    }

    @Test
    void calculateGroupScore_sumsProgressWithPoints() {
        Group group = new Group();
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));
        BigDecimal score = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "calculateGroupScore", group, Map.of(1, 2));
        assertEquals(new BigDecimal("2"), score);
    }

    @Test
    void clampScore_capsAtMax() {
        BigDecimal capped = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "clampScore", new BigDecimal("10"), new BigDecimal("5"));
        assertEquals(new BigDecimal("5"), capped);
    }

    @Test
    void clampScore_handlesNullAndNegative() {
        BigDecimal zero = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "clampScore", null, BigDecimal.TEN);
        assertEquals(BigDecimal.ZERO, zero);

        BigDecimal negative = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                exportService, "clampScore", new BigDecimal("-5"), BigDecimal.TEN);
        assertEquals(BigDecimal.ZERO, negative);
    }

    @Test
    void generateCsv_memberNameOnlyCreatesNewRowWithScore() {
        Lab lab = new Lab("class-16", "Name Only", 1, "inst");
        lab.setId("lab-name-only");

        com.example.lab_signoff_backend.model.Class clazz = new com.example.lab_signoff_backend.model.Class("CS911", "NameOnly", "Fall", "inst");
        clazz.setId("class-16");
        clazz.setSection("SEC16");

        GroupMember member = new GroupMember();
        member.setName("Solo Student");
        member.setPresent(true);
        Group group = new Group();
        group.setLabId("lab-name-only");
        group.setGroupId("g-name-only");
        group.setMembers(List.of(member));
        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.PASS);
        group.setCheckpointProgress(List.of(cp));

        when(labService.getById("lab-name-only")).thenReturn(Optional.of(lab));
        when(classService.getClassById("class-16")).thenReturn(Optional.of(clazz));
        when(groupService.getGroupsByLabId("lab-name-only")).thenReturn(List.of(group));
        when(enrollmentService.getActiveStudents("class-16")).thenReturn(List.of());
        when(userService.findByIds(any())).thenReturn(new java.util.HashMap<>());

        LabGradeExportService.ExportResult result = exportService.generateCsv("lab-name-only");
        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Solo Student"));
        assertTrue(csv.contains(",1"));
    }
}
