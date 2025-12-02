package com.example.lab_signoff_backend.model;

import com.example.lab_signoff_backend.model.embedded.CheckpointDefinition;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.embedded.CanvasMetadata;
import com.example.lab_signoff_backend.model.enums.EnrollmentRole;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ModelHelpersTest {

    @Test
    void groupLifecycleAndMembersUpdateStatusAndTimestamps() {
        Group group = new Group();
        group.setGroupId("G1");
        group.setLabId("LAB1");

        GroupMember member = new GroupMember();
        member.setUserId("u1");
        group.addMember(member);
        assertEquals(1, group.getMembers().size());

        group.removeMember("u1");
        assertTrue(group.getMembers().isEmpty());

        group.startProgress();
        assertEquals(GroupStatus.IN_PROGRESS, group.getStatus());

        group.signOff();
        assertEquals(GroupStatus.SIGNED_OFF, group.getStatus());

        group.complete();
        assertEquals(GroupStatus.COMPLETED, group.getStatus());
    }

    @Test
    void labCreatesCheckpointsAndRegeneratesJoinCode() {
        Lab lab = new Lab("class-1", "Lab Title", 2, "creator");
        assertEquals(2, lab.getCheckpoints().size());

        String firstCode = lab.getJoinCode();
        lab.regenerateJoinCode();
        String secondCode = lab.getJoinCode();

        assertNotEquals(firstCode, secondCode);
        assertEquals(6, secondCode.length());

        lab.setPoints(3);
        lab.setCheckpoints(List.of(new CheckpointDefinition()));
        lab.setTitle("Updated");
        assertEquals("Updated", lab.getTitle());
    }

    @Test
    void classRosterStaffFlagsWork() {
        Class clazz = new Class("CS301", "Networks", "Fall", "prof-1");
        clazz.addStudentToRoster("student-1");
        clazz.addTA("ta-1");

        assertTrue(clazz.isStudentInRoster("student-1"));
        assertTrue(clazz.isTA("ta-1"));
        assertTrue(clazz.isInstructor("prof-1"));
        assertTrue(clazz.isStaff("ta-1"));

        clazz.removeStudentFromRoster("student-1");
        assertFalse(clazz.isStudentInRoster("student-1"));

        clazz.removeTA("ta-1");
        assertFalse(clazz.isTA("ta-1"));
    }

    @Test
    void enrollmentRoleAndStatusHelpers() {
        Enrollment enrollment = new Enrollment("u10", "c10", EnrollmentRole.STUDENT);
        assertTrue(enrollment.isStudent());
        assertFalse(enrollment.isTA());
        assertTrue(enrollment.isActive());

        enrollment.upgradeToTA("prof");
        assertTrue(enrollment.isTA());
        assertEquals("prof", enrollment.getUpgradeRequestedBy());

        enrollment.drop();
        assertFalse(enrollment.isActive());

        enrollment.complete();
        assertEquals("COMPLETED", enrollment.getStatus().name());
    }

    @Test
    void helpQueueItemStateChangesAndUrgency() {
        HelpQueueItem item = new HelpQueueItem("labX", "groupX", "raiser", 1);
        assertTrue(item.isWaiting());
        item.claim("ta");
        assertTrue(item.isClaimed());
        item.resolve();
        assertTrue(item.isResolved());
        item.cancel();
        assertTrue(item.isCancelled());
        item.setUrgent();
        assertTrue(item.isUrgent());
        item.setNormal();
        assertFalse(item.isUrgent());
    }

    @Test
    void embeddedDtosStoreAndReturnData() {
        CheckpointDefinition def = new CheckpointDefinition();
        def.setNumber(1);
        def.setName("CP1");
        def.setDescription("desc");
        def.setPoints(2);
        def.setRequired(true);
        assertEquals("CP1", def.getName());

        CheckpointProgress progress = new CheckpointProgress();
        progress.setCheckpointNumber(2);
        progress.setStatus(SignoffAction.RETURN);
        progress.setNotes("note");
        assertEquals(2, progress.getCheckpointNumber());
        assertEquals(SignoffAction.RETURN, progress.getStatus());

        GroupMember member = new GroupMember();
        member.setUserId("u1");
        member.setName("Member");
        member.setEmail("m@example.com");
        assertEquals("Member", member.getName());

        CanvasMetadata meta = new CanvasMetadata("lineItem", "course");
        meta.setContextId("ctx");
        meta.setDeploymentId("dep");
        meta.setResourceLinkId("res");
        assertEquals("course", meta.getCourseId());
        assertEquals("ctx", meta.getContextId());
    }

    @Test
    void userRoleHelpersWork() {
        User user = new User("auth0|u", "u@example.com", "User One", null, List.of("Teacher", "Admin"));
        user.setFirstName("User");
        user.setLastName("One");
        assertTrue(user.isTeacher());
        assertTrue(user.isStaffOrAdmin());
        assertFalse(user.isStudent());
        user.updateLastLogin();
    }

    @Test
    void labAndClassAccessorsCoverFields() {
        Lab lab = new Lab("class-2", "Lab Title", 4, "creator");
        lab.setId("lab-x");
        lab.setDescription("desc");
        lab.setStatus(com.example.lab_signoff_backend.model.enums.LabStatus.ACTIVE);
        lab.setJoinCode("JOINME");
        lab.setAutoRandomize(false);
        lab.setMinGroupSize(2);
        lab.setMaxGroupSize(5);
        lab.updateTimestamp();
        lab.regenerateJoinCode();
        assertEquals("Lab Title", lab.getTitle());
        assertEquals(2, lab.getMinGroupSize());

        Class clazz = new Class("CSC100", "Intro", "Fall", "inst");
        clazz.setId("class-x");
        clazz.setSection("001");
        clazz.addStudentToRoster("Alice");
        clazz.addTA("ta1");
        assertTrue(clazz.isStudentInRoster("Alice"));
        assertTrue(clazz.isTA("ta1"));
        clazz.removeStudentFromRoster("Alice");
        clazz.removeTA("ta1");
        assertFalse(clazz.isStudentInRoster("Alice"));
    }

    @Test
    void signoffEventAndCheckpointUpdateAccessors() {
        SignoffEvent event = new SignoffEvent();
        event.setId("evt1");
        event.setLabId("lab1");
        event.setGroupId("g1");
        event.setAction(SignoffAction.PASS);
        event.setPerformedBy("ta");
        event.setCheckpointNumber(1);
        event.setNotes("note");
        assertEquals("evt1", event.getId());
        assertEquals("g1", event.getGroupId());

        CheckpointUpdate update = new CheckpointUpdate("lab1", "g1", 1, "PASS");
        update.setSignedOffByName("ta");
        update.setNotes("n");
        update.setTimestamp(Instant.now());
        assertEquals("PASS", update.getStatus());
        assertEquals("g1", update.getGroupId());
    }
}
