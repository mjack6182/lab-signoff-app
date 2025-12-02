package com.example.lab_signoff_backend.websocket;

import com.example.lab_signoff_backend.model.CheckpointUpdate;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import com.example.lab_signoff_backend.model.websocket.GroupStatusUpdate;
import com.example.lab_signoff_backend.model.websocket.HelpQueueUpdate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LabWebSocketControllerTest {

    private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    private final LabWebSocketController controller = new LabWebSocketController();

    LabWebSocketControllerTest() {
        ReflectionTestUtils.setField(controller, "messagingTemplate", template);
    }

    @Test
    void broadcastCheckpointUpdate_sendsToTopic() {
        CheckpointUpdate update = new CheckpointUpdate("lab1", "g1", 1, "PASS");
        controller.broadcastCheckpointUpdate("lab1", update);
        verify(template).convertAndSend("/topic/group-updates", update);
    }

    @Test
    void broadcastGroupPassed_sendsToSpecificTopics() {
        controller.broadcastGroupPassed("lab1", "g1");
        verify(template).convertAndSend(eq("/topic/group-updates/g1"), any(Object.class));
        verify(template).convertAndSend(eq("/topic/group-updates"), any(Object.class));
    }

    @Test
    void broadcastGroupStatusUpdate_sendsLabAndGroupTopics() {
        GroupStatusUpdate update = new GroupStatusUpdate("lab1", "g1", GroupStatus.IN_PROGRESS);
        controller.broadcastGroupStatusUpdate("lab1", update);
        verify(template).convertAndSend("/topic/labs/lab1/groups", update);
        verify(template).convertAndSend("/topic/groups/g1/status", update);
    }

    @Test
    void broadcastHelpQueueUpdate_sendsLabAndGroupIfPresent() {
        HelpQueueUpdate update = new HelpQueueUpdate("id1", "lab1", "g1", com.example.lab_signoff_backend.model.enums.HelpQueueStatus.WAITING);
        controller.broadcastHelpQueueUpdate("lab1", update);
        verify(template).convertAndSend("/topic/labs/lab1/help-queue", update);
        verify(template).convertAndSend("/topic/groups/g1/help-queue", update);
    }

    @Test
    void legacyBroadcastsStillSend() {
        controller.broadcastCheckpointUpdate("g1", 1, "PASS");
        controller.broadcastGroupPassed("g2");
        verify(template, atLeastOnce()).convertAndSend(eq("/topic/group-updates"), any(Object.class));
    }

    @Test
    void broadcastGroupsRandomized_sendsMessage() {
        controller.broadcastGroupsRandomized("lab1");
        ArgumentCaptor<LabWebSocketController.GroupsRandomizedMessage> captor = ArgumentCaptor.forClass(LabWebSocketController.GroupsRandomizedMessage.class);
        verify(template).convertAndSend(eq("/topic/labs/lab1/groups-randomized"), captor.capture());
        assertEquals("lab1", captor.getValue().getLabId());
    }
}
