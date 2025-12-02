package com.example.lab_signoff_backend.model.websocket;

import com.example.lab_signoff_backend.model.enums.HelpQueueStatus;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebsocketDtoTest {

    @Test
    void groupStatusUpdateStoresFields() {
        GroupStatusUpdate update = new GroupStatusUpdate("lab1", "g1", GroupStatus.IN_PROGRESS);
        update.setStatus(GroupStatus.SIGNED_OFF);
        assertEquals("lab1", update.getLabId());
        assertEquals("g1", update.getGroupId());
        assertEquals(GroupStatus.SIGNED_OFF, update.getStatus());
    }

    @Test
    void helpQueueUpdateStoresFields() {
        HelpQueueUpdate update = new HelpQueueUpdate("id1", "lab1", "group1", HelpQueueStatus.WAITING);
        update.setPreviousStatus(HelpQueueStatus.CLAIMED);
        update.setPosition(3);
        update.setRequestedBy("student");
        update.setRequestedByName("Student");
        update.setClaimedBy("ta");
        update.setClaimedByName("TA");
        update.setDescription("desc");

        assertEquals("id1", update.getId());
        assertEquals("lab1", update.getLabId());
        assertEquals("group1", update.getGroupId());
        assertEquals(HelpQueueStatus.WAITING, update.getStatus());
        assertEquals(HelpQueueStatus.CLAIMED, update.getPreviousStatus());
        assertEquals(3, update.getPosition());
        assertEquals("student", update.getRequestedBy());
        assertEquals("ta", update.getClaimedBy());
    }
}
