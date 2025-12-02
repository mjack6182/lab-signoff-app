package com.example.lab_signoff_backend.model.websocket;

import com.example.lab_signoff_backend.model.enums.GroupStatus;
import com.example.lab_signoff_backend.model.enums.HelpQueuePriority;
import com.example.lab_signoff_backend.model.enums.HelpQueueStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebsocketModelsTest {

    @Test
    void groupStatusUpdate_exposesFields() {
        GroupStatusUpdate update = new GroupStatusUpdate("lab1", "g1", GroupStatus.IN_PROGRESS);
        update.setPreviousStatus(GroupStatus.FORMING);
        update.setPerformedBy("user-1");
        update.setPerformedByName("User One");
        update.setTotalScore(10);
        update.setFinalGrade("A");

        assertEquals("lab1", update.getLabId());
        assertEquals("g1", update.getGroupId());
        assertEquals(GroupStatus.IN_PROGRESS, update.getStatus());
        assertEquals(GroupStatus.FORMING, update.getPreviousStatus());
        assertEquals("user-1", update.getPerformedBy());
        assertEquals("User One", update.getPerformedByName());
        assertEquals(10, update.getTotalScore());
        assertEquals("A", update.getFinalGrade());
    }

    @Test
    void helpQueueUpdate_returnsQueueItems() {
        HelpQueueUpdate queueUpdate = new HelpQueueUpdate("id1", "lab2", "g1", HelpQueueStatus.WAITING);
        queueUpdate.setPreviousStatus(HelpQueueStatus.CANCELLED);
        queueUpdate.setPriority(HelpQueuePriority.URGENT);
        queueUpdate.setPosition(3);
        queueUpdate.setRequestedBy("user-2");
        queueUpdate.setRequestedByName("User Two");
        queueUpdate.setClaimedBy("helper-1");
        queueUpdate.setClaimedByName("Helper One");
        queueUpdate.setTimestamp(Instant.ofEpochSecond(50));
        queueUpdate.setDescription("Need help");

        assertEquals("lab2", queueUpdate.getLabId());
        assertEquals("g1", queueUpdate.getGroupId());
        assertEquals(HelpQueueStatus.WAITING, queueUpdate.getStatus());
        assertEquals(HelpQueueStatus.CANCELLED, queueUpdate.getPreviousStatus());
        assertEquals(HelpQueuePriority.URGENT, queueUpdate.getPriority());
        assertEquals(3, queueUpdate.getPosition());
        assertEquals("user-2", queueUpdate.getRequestedBy());
        assertEquals("User Two", queueUpdate.getRequestedByName());
        assertEquals("helper-1", queueUpdate.getClaimedBy());
        assertEquals("Helper One", queueUpdate.getClaimedByName());
        assertEquals(Instant.ofEpochSecond(50), queueUpdate.getTimestamp());
        assertEquals("Need help", queueUpdate.getDescription());
    }
}
