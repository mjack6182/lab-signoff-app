package com.example.lab_signoff_backend.websocket;

import com.example.lab_signoff_backend.model.CheckpointUpdate;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import com.example.lab_signoff_backend.model.websocket.GroupStatusUpdate;
import com.example.lab_signoff_backend.model.websocket.HelpQueueUpdate;
import com.example.lab_signoff_backend.websocket.LabWebSocketController.GroupsRandomizedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class LabWebSocketControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private LabWebSocketController controller;

    @BeforeEach
    void setUp() {
        // no-op, @InjectMocks + @Mock already wired
    }

    @Test
    void broadcastCheckpointUpdate_sendsToGroupUpdatesTopic() {
        String labId = "lab-123";
        CheckpointUpdate update =
                new CheckpointUpdate(labId, "group-1", 1, "PASS");

        controller.broadcastCheckpointUpdate(labId, update);

        verify(messagingTemplate)
                .convertAndSend("/topic/group-updates", update);
    }

    @Test
    void broadcastGroupPassed_sendsToSpecificAndGenericTopics() {
        String labId = "lab-456";
        String groupId = "group-42";

        ArgumentCaptor<GroupStatusUpdate> captor =
                ArgumentCaptor.forClass(GroupStatusUpdate.class);

        controller.broadcastGroupPassed(labId, groupId);

        // specific group topic
        verify(messagingTemplate)
                .convertAndSend(eq("/topic/group-updates/" + groupId), captor.capture());

        GroupStatusUpdate sent = captor.getValue();
        assertEquals(labId, sent.getLabId());
        assertEquals(groupId, sent.getGroupId());
        assertEquals(GroupStatus.SIGNED_OFF, sent.getStatus());

        // generic feed
        verify(messagingTemplate)
                .convertAndSend("/topic/group-updates", sent);

        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void broadcastGroupStatusUpdate_sendsToLabAndGroupStatusTopics() {
        String labId = "lab-789";
        String groupId = "group-99";
        GroupStatusUpdate update =
                new GroupStatusUpdate(labId, groupId, GroupStatus.IN_PROGRESS);

        controller.broadcastGroupStatusUpdate(labId, update);

        verify(messagingTemplate)
                .convertAndSend("/topic/labs/" + labId + "/groups", update);
        verify(messagingTemplate)
                .convertAndSend("/topic/groups/" + groupId + "/status", update);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void broadcastHelpQueueUpdate_withGroupId_sendsToLabAndGroupQueueTopics() {
        String labId = "lab-555";
        String groupId = "group-12";

        HelpQueueUpdate update = mock(HelpQueueUpdate.class);
        when(update.getGroupId()).thenReturn(groupId);

        controller.broadcastHelpQueueUpdate(labId, update);

        verify(messagingTemplate)
                .convertAndSend("/topic/labs/" + labId + "/help-queue", update);
        verify(messagingTemplate)
                .convertAndSend("/topic/groups/" + groupId + "/help-queue", update);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void broadcastHelpQueueUpdate_withoutGroupId_sendsOnlyLabQueueTopic() {
        String labId = "lab-555";

        HelpQueueUpdate update = mock(HelpQueueUpdate.class);
        when(update.getGroupId()).thenReturn(null);

        controller.broadcastHelpQueueUpdate(labId, update);

        verify(messagingTemplate)
                .convertAndSend("/topic/labs/" + labId + "/help-queue", update);
        // no group-specific send
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void legacyBroadcastCheckpointUpdate_usesCheckpointUpdateAndGroupUpdatesTopic() {
        String groupId = "legacy-group";
        int cpNum = 2;
        String status = "PASS";

        controller.broadcastCheckpointUpdate(groupId, cpNum, status);

        verify(messagingTemplate)
                .convertAndSend(eq("/topic/group-updates"), any(CheckpointUpdate.class));
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void legacyBroadcastGroupPassed_sendsSignedOffStatusToGenericGroupUpdates() {
        String groupId = "legacy-group";

        ArgumentCaptor<GroupStatusUpdate> captor =
                ArgumentCaptor.forClass(GroupStatusUpdate.class);

        controller.broadcastGroupPassed(groupId);

        verify(messagingTemplate)
                .convertAndSend(eq("/topic/group-updates"), captor.capture());

        GroupStatusUpdate sent = captor.getValue();
        assertNull(sent.getLabId()); // as in legacy method
        assertEquals(groupId, sent.getGroupId());
        assertEquals(GroupStatus.SIGNED_OFF, sent.getStatus());

        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void broadcastGroupsRandomized_sendsGroupsRandomizedMessageToLabTopic() {
        String labId = "lab-xyz";

        ArgumentCaptor<GroupsRandomizedMessage> captor =
                ArgumentCaptor.forClass(GroupsRandomizedMessage.class);

        controller.broadcastGroupsRandomized(labId);

        verify(messagingTemplate)
                .convertAndSend(eq("/topic/labs/" + labId + "/groups-randomized"),
                        captor.capture());

        GroupsRandomizedMessage msg = captor.getValue();
        assertEquals(labId, msg.getLabId());
        assertEquals("Groups have been randomized", msg.getMessage());
        assertTrue(msg.getTimestamp() > 0);

        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void testBroadcast_createsCheckpointUpdateAndReturnsSuccessMessage() {
        String response = controller.testBroadcast();

        assertTrue(response.contains("Test WebSocket broadcast sent!"));

        verify(messagingTemplate)
                .convertAndSend(eq("/topic/group-updates"), any(CheckpointUpdate.class));
        verifyNoMoreInteractions(messagingTemplate);
    }
}
