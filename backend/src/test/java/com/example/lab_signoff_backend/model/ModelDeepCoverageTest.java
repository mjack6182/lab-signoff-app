package com.example.lab_signoff_backend.model;

import com.example.lab_signoff_backend.model.embedded.CanvasMetadata;
import com.example.lab_signoff_backend.model.embedded.CheckpointDefinition;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.enums.*;
import com.example.lab_signoff_backend.model.websocket.GroupStatusUpdate;
import com.example.lab_signoff_backend.model.websocket.HelpQueueUpdate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelDeepCoverageTest {

    @Test
    void exerciseCoreModels() {
        // Class and Lab
        Class clazz = new Class("CSC200", "Data Structures", "Fall", "inst");
        clazz.setId("class-1");
        clazz.setSection("002");
        clazz.setArchived(true);
        clazz.addStudentToRoster("Alice");
        clazz.addTA("ta1");
        assertTrue(clazz.getArchived());
        assertTrue(clazz.isStaff("ta1"));
        clazz.removeStudentFromRoster("Alice");
        clazz.removeTA("ta1");

        Lab lab = new Lab("class-1", "Lab DS", 2, "inst");
        lab.setId("lab-1");
        lab.setStatus(LabStatus.ACTIVE);
        lab.setAutoRandomize(false);
        lab.setMaxGroupSize(4);
        lab.setMinGroupSize(2);
        lab.setPoints(3);
        lab.updateTimestamp();
        lab.regenerateJoinCode();
        assertEquals(LabStatus.ACTIVE, lab.getStatus());

        // Group, members, progress
        Group group = new Group();
        group.setId("g1");
        group.setGroupId("G-1");
        group.setLabId("lab-1");
        group.setStatus(GroupStatus.IN_PROGRESS);
        group.setTotalScore(BigDecimal.TEN);
        group.setFinalGrade(BigDecimal.ONE);
        group.updateTimestamp();
        group.complete();
        group.signOff();

        GroupMember gm = new GroupMember();
        gm.setUserId("u1");
        gm.setName("User One");
        gm.setEmail("u1@example.com");
        gm.setPresent(true);
        group.setMembers(List.of(gm));

        CheckpointProgress cp = new CheckpointProgress();
        cp.setCheckpointNumber(1);
        cp.setStatus(SignoffAction.PASS);
        cp.setPointsAwarded(5);
        cp.setSignedOffBy("ta");
        cp.setTimestamp(Instant.now());
        cp.setNotes("done");
        group.setCheckpointProgress(List.of(cp));
        assertEquals(SignoffAction.PASS, group.getCheckpointProgress().getFirst().getStatus());

        // Enrollment
        Enrollment enrollment = new Enrollment("u1", "class-1", EnrollmentRole.STUDENT);
        enrollment.setRoleAndUpdate(EnrollmentRole.TA, "teacher");
        enrollment.upgradeToTA("teacher");
        enrollment.drop();
        enrollment.complete();
        assertTrue(enrollment.isStaff());

        // Help queue item
        HelpQueueItem item = new HelpQueueItem("lab-1", "g1", "u1", 1);
        item.setDescription("help");
        item.claim("ta1");
        item.resolve();
        item.cancel();
        item.setUrgent();
        item.setNormal();
        assertTrue(item.isCancelled());

        // Canvas metadata and checkpoint definition
        CanvasMetadata meta = new CanvasMetadata("line", "course");
        meta.setContextId("ctx");
        meta.setDeploymentId("dep");
        meta.setResourceLinkId("res");
        assertEquals("dep", meta.getDeploymentId());

        CheckpointDefinition def = new CheckpointDefinition();
        def.setNumber(2);
        def.setName("Checkpoint 2");
        def.setDescription("desc");
        def.setPoints(3);
        def.setRequired(false);
        assertFalse(def.getRequired());

        // websocket DTOs
        GroupStatusUpdate gs = new GroupStatusUpdate("lab-1", "g1", GroupStatus.FORMING);
        gs.setStatus(GroupStatus.SIGNED_OFF);
        assertEquals(GroupStatus.SIGNED_OFF, gs.getStatus());

        HelpQueueUpdate hqu = new HelpQueueUpdate("id1", "lab-1", "g1", HelpQueueStatus.WAITING);
        hqu.setPriority(HelpQueuePriority.URGENT);
        hqu.setPosition(2);
        assertEquals(HelpQueuePriority.URGENT, hqu.getPriority());
    }
}
