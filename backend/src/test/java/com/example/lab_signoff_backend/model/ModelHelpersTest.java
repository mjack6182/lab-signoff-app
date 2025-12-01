package com.example.lab_signoff_backend.model;

import com.example.lab_signoff_backend.model.embedded.CheckpointDefinition;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.enums.EnrollmentRole;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

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
}
