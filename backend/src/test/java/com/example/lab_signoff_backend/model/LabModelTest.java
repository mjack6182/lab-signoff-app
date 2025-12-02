package com.example.lab_signoff_backend.model;

import com.example.lab_signoff_backend.model.embedded.CheckpointDefinition;
import com.example.lab_signoff_backend.model.enums.LabStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LabModelTest {

    @Test
    void setPoints_reinitializesCheckpoints() {
        Lab lab = new Lab("class-1", "Lab", 1, "creator");
        lab.setPoints(3);

        List<CheckpointDefinition> checkpoints = lab.getCheckpoints();
        assertEquals(3, checkpoints.size());
        assertEquals("Checkpoint 3", checkpoints.get(2).getName());
    }

    @Test
    void statusTransitions_updateFlagsAndTimestamps() {
        Lab lab = new Lab("class-1", "Lab", 1, "creator");

        lab.activate();
        assertEquals(LabStatus.ACTIVE, lab.getStatus());
        assertNotNull(lab.getStartTime());

        lab.close();
        assertEquals(LabStatus.CLOSED, lab.getStatus());
        assertNotNull(lab.getEndTime());

        lab.archive();
        assertEquals(LabStatus.ARCHIVED, lab.getStatus());
    }

    @Test
    void isJoinable_respectsEndTime() {
        Lab lab = new Lab("class-1", "Lab", 1, "creator");
        lab.activate();
        lab.setEndTime(Instant.now().plusSeconds(60));
        assertTrue(lab.isJoinable());

        lab.setEndTime(Instant.now().minusSeconds(5));
        assertFalse(lab.isJoinable());
    }

    @Test
    void regenerateJoinCode_changesValue() {
        Lab lab = new Lab("class-1", "Lab", 1, "creator");
        String old = lab.getJoinCode();
        lab.regenerateJoinCode();
        assertNotEquals(old, lab.getJoinCode());
    }

    @Test
    void defaultStatusFlags() {
        Lab lab = new Lab();
        assertTrue(lab.isDraft());
        lab.setStatus(LabStatus.ACTIVE);
        assertFalse(lab.isDraft());
    }

    @Test
    void isJoinable_falseWhenNotActive() {
        Lab lab = new Lab();
        lab.setStatus(LabStatus.DRAFT);
        lab.setEndTime(Instant.now().plusSeconds(60));
        assertFalse(lab.isJoinable());
    }

    @Test
    void setPoints_nullDoesNotReinitialize() {
        Lab lab = new Lab("class-1", "Lab", 2, "creator");
        lab.setPoints(null);
        assertEquals(2, lab.getCheckpoints().size());
    }
}
