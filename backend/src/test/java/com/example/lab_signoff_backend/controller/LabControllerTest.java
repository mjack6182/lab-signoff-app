package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.SignoffEvent;
import com.example.lab_signoff_backend.service.GroupService;
import com.example.lab_signoff_backend.service.LabService;
import com.example.lab_signoff_backend.service.SignoffEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LabController
 *
 * Tests all endpoints including the new pass and return endpoints,
 * ensuring proper validation, error handling, and audit logging.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class LabControllerTest {

    @Mock
    private LabService labService;

    @Mock
    private GroupService groupService;

    @Mock
    private SignoffEventService signoffEventService;

    @InjectMocks
    private LabController labController;

    private Lab testLab;
    private Group testGroup;
    private SignoffEvent testEvent;

    @BeforeEach
    void setUp() {
        // Set up test data
        testLab = new Lab("lab1", "course1", "lineItem1");

        testGroup = new Group();
        testGroup.setId("group-mongo-id-1");
        testGroup.setGroupId("group1");
        testGroup.setLabId("lab1");
        testGroup.setMembers(Arrays.asList("student1", "student2"));
        testGroup.setStatus("in-progress");

        testEvent = new SignoffEvent();
        testEvent.setId("event1");
        testEvent.setLabId("lab1");
        testEvent.setGroupId("group1");
        testEvent.setAction("PASS");
        testEvent.setTimestamp(Instant.now());
        testEvent.setPerformedBy("instructor1");
    }

    /**
     * Test: Successfully passing a group
     * Verifies that the group status is updated and an audit event is created
     */
    @Test
    void testPassGroup_Success() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(Arrays.asList(testGroup));
        when(groupService.upsert(any(Group.class))).thenReturn(testGroup);
        when(signoffEventService.createEvent(anyString(), anyString(), anyString(),
                anyString(), anyString(), any())).thenReturn(testEvent);

        // Act
        ResponseEntity<?> response = labController.passGroup("lab1", "group1", "instructor1", "Good work!");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify group status was updated
        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupService).upsert(groupCaptor.capture());
        assertEquals("passed", groupCaptor.getValue().getStatus());

        // Verify audit event was created
        verify(signoffEventService).createEvent(
                eq("lab1"),
                eq("group1"),
                eq("PASS"),
                eq("instructor1"),
                eq("Good work!"),
                isNull()
        );
    }

    /**
     * Test: Passing a group with empty lab ID
     * Should return 400 Bad Request
     */
    @Test
    void testPassGroup_EmptyLabId() {
        // Act
        ResponseEntity<?> response = labController.passGroup("", "group1", "instructor1", null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Lab ID cannot be empty", response.getBody());

        // Verify no database operations were performed
        verify(labService, never()).labExists(anyString());
        verify(groupService, never()).upsert(any());
        verify(signoffEventService, never()).createEvent(anyString(), anyString(),
                anyString(), anyString(), anyString(), any());
    }

    /**
     * Test: Passing a group with empty group ID
     * Should return 400 Bad Request
     */
    @Test
    void testPassGroup_EmptyGroupId() {
        // Act
        ResponseEntity<?> response = labController.passGroup("lab1", "", "instructor1", null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Group ID cannot be empty", response.getBody());

        // Verify no database operations were performed
        verify(labService, never()).labExists(anyString());
        verify(groupService, never()).upsert(any());
        verify(signoffEventService, never()).createEvent(anyString(), anyString(),
                anyString(), anyString(), anyString(), any());
    }

    /**
     * Test: Passing a group when lab does not exist
     * Should return 404 Not Found
     */
    @Test
    void testPassGroup_LabNotFound() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(false);

        // Act
        ResponseEntity<?> response = labController.passGroup("lab1", "group1", "instructor1", null);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Lab with ID lab1 not found"));

        // Verify group was not updated
        verify(groupService, never()).upsert(any());
        verify(signoffEventService, never()).createEvent(anyString(), anyString(),
                anyString(), anyString(), anyString(), any());
    }

    /**
     * Test: Passing a group when group does not exist
     * Should return 404 Not Found
     */
    @Test
    void testPassGroup_GroupNotFound() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(Arrays.asList()); // Empty list

        // Act
        ResponseEntity<?> response = labController.passGroup("lab1", "group1", "instructor1", null);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Group with ID group1 not found"));

        // Verify no updates were made
        verify(groupService, never()).upsert(any());
        verify(signoffEventService, never()).createEvent(anyString(), anyString(),
                anyString(), anyString(), anyString(), any());
    }

    /**
     * Test: Passing a group without performedBy parameter
     * Should default to "system"
     */
    @Test
    void testPassGroup_NoPerformedBy() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(Arrays.asList(testGroup));
        when(groupService.upsert(any(Group.class))).thenReturn(testGroup);
        when(signoffEventService.createEvent(eq("lab1"), eq("group1"), eq("PASS"),
                eq("system"), isNull(), isNull())).thenReturn(testEvent);

        // Act
        ResponseEntity<?> response = labController.passGroup("lab1", "group1", null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify audit event was created with "system" as performer
        verify(signoffEventService).createEvent(
                eq("lab1"),
                eq("group1"),
                eq("PASS"),
                eq("system"),
                isNull(),
                isNull()
        );
    }

    /**
     * Test: Successfully returning a group
     * Verifies that the group status is updated and an audit event is created
     */
    @Test
    void testReturnGroup_Success() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(Arrays.asList(testGroup));
        when(groupService.upsert(any(Group.class))).thenReturn(testGroup);

        SignoffEvent returnEvent = new SignoffEvent();
        returnEvent.setId("event2");
        returnEvent.setLabId("lab1");
        returnEvent.setGroupId("group1");
        returnEvent.setAction("RETURN");
        returnEvent.setTimestamp(Instant.now());
        returnEvent.setPerformedBy("instructor1");

        when(signoffEventService.createEvent(anyString(), anyString(), anyString(),
                anyString(), anyString(), any())).thenReturn(returnEvent);

        // Act
        ResponseEntity<?> response = labController.returnGroup("lab1", "group1",
                "instructor1", "Needs more work on checkpoint 2");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify group status was updated
        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupService).upsert(groupCaptor.capture());
        assertEquals("returned", groupCaptor.getValue().getStatus());

        // Verify audit event was created
        verify(signoffEventService).createEvent(
                eq("lab1"),
                eq("group1"),
                eq("RETURN"),
                eq("instructor1"),
                eq("Needs more work on checkpoint 2"),
                isNull()
        );
    }

    /**
     * Test: Returning a group with empty lab ID
     * Should return 400 Bad Request
     */
    @Test
    void testReturnGroup_EmptyLabId() {
        // Act
        ResponseEntity<?> response = labController.returnGroup("", "group1", "instructor1", null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Lab ID cannot be empty", response.getBody());

        // Verify no database operations were performed
        verify(labService, never()).labExists(anyString());
        verify(groupService, never()).upsert(any());
        verify(signoffEventService, never()).createEvent(anyString(), anyString(),
                anyString(), anyString(), anyString(), any());
    }

    /**
     * Test: Returning a group with empty group ID
     * Should return 400 Bad Request
     */
    @Test
    void testReturnGroup_EmptyGroupId() {
        // Act
        ResponseEntity<?> response = labController.returnGroup("lab1", "", "instructor1", null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Group ID cannot be empty", response.getBody());

        // Verify no database operations were performed
        verify(labService, never()).labExists(anyString());
        verify(groupService, never()).upsert(any());
        verify(signoffEventService, never()).createEvent(anyString(), anyString(),
                anyString(), anyString(), anyString(), any());
    }

    /**
     * Test: Returning a group when lab does not exist
     * Should return 404 Not Found
     */
    @Test
    void testReturnGroup_LabNotFound() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(false);

        // Act
        ResponseEntity<?> response = labController.returnGroup("lab1", "group1", "instructor1", null);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Lab with ID lab1 not found"));

        // Verify group was not updated
        verify(groupService, never()).upsert(any());
        verify(signoffEventService, never()).createEvent(anyString(), anyString(),
                anyString(), anyString(), anyString(), any());
    }

    /**
     * Test: Returning a group when group does not exist
     * Should return 404 Not Found
     */
    @Test
    void testReturnGroup_GroupNotFound() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(Arrays.asList()); // Empty list

        // Act
        ResponseEntity<?> response = labController.returnGroup("lab1", "group1", "instructor1", null);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Group with ID group1 not found"));

        // Verify no updates were made
        verify(groupService, never()).upsert(any());
        verify(signoffEventService, never()).createEvent(anyString(), anyString(),
                anyString(), anyString(), anyString(), any());
    }

    /**
     * Test: Returning a group without performedBy parameter
     * Should default to "system"
     */
    @Test
    void testReturnGroup_NoPerformedBy() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(Arrays.asList(testGroup));
        when(groupService.upsert(any(Group.class))).thenReturn(testGroup);

        SignoffEvent returnEvent = new SignoffEvent();
        returnEvent.setAction("RETURN");
        when(signoffEventService.createEvent(eq("lab1"), eq("group1"), eq("RETURN"),
                eq("system"), isNull(), isNull())).thenReturn(returnEvent);

        // Act
        ResponseEntity<?> response = labController.returnGroup("lab1", "group1", null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify audit event was created with "system" as performer
        verify(signoffEventService).createEvent(
                eq("lab1"),
                eq("group1"),
                eq("RETURN"),
                eq("system"),
                isNull(),
                isNull()
        );
    }

    /**
     * Test: Handling exception during group update in pass endpoint
     * Should return 500 Internal Server Error
     */
    @Test
    void testPassGroup_UpdateException() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(Arrays.asList(testGroup));
        when(groupService.upsert(any(Group.class))).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = labController.passGroup("lab1", "group1", "instructor1", null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error updating group status"));

        // Verify no audit event was created
        verify(signoffEventService, never()).createEvent(anyString(), anyString(),
                anyString(), anyString(), anyString(), any());
    }

    /**
     * Test: Handling exception during audit logging in pass endpoint
     * Should return 206 Partial Content (group updated but audit failed)
     */
    @Test
    void testPassGroup_AuditException() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(true);
        when(groupService.getAll()).thenReturn(Arrays.asList(testGroup));
        when(groupService.upsert(any(Group.class))).thenReturn(testGroup);
        when(signoffEventService.createEvent(anyString(), anyString(), anyString(),
                anyString(), anyString(), any())).thenThrow(new RuntimeException("Audit logging failed"));

        // Act
        ResponseEntity<?> response = labController.passGroup("lab1", "group1", "instructor1", null);

        // Assert
        assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("audit logging failed"));

        // Verify group was still updated
        verify(groupService).upsert(any(Group.class));
    }

    /**
     * Test: Get all labs
     */
    @Test
    void testGetLabs() {
        // Arrange
        List<Lab> labs = Arrays.asList(testLab);
        when(labService.getAll()).thenReturn(labs);

        // Act
        List<Lab> result = labController.getLabs();

        // Assert
        assertEquals(1, result.size());
        assertEquals("lab1", result.get(0).getId());
        verify(labService).getAll();
    }

    /**
     * Test: Create or update lab
     */
    @Test
    void testCreateOrUpdateLab() {
        // Arrange
        when(labService.upsert(any(Lab.class))).thenReturn(testLab);

        // Act
        Lab result = labController.createOrUpdateLab(testLab);

        // Assert
        assertEquals("lab1", result.getId());
        verify(labService).upsert(testLab);
    }

    /**
     * Test: Get groups by lab ID - success
     */
    @Test
    void testGetGroupsByLabId_Success() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(true);
        when(groupService.getGroupsByLabId("lab1")).thenReturn(Arrays.asList(testGroup));

        // Act
        ResponseEntity<?> response = labController.getGroupsByLabId("lab1");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Group> groups = (List<Group>) response.getBody();
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertEquals("group1", groups.get(0).getGroupId());
    }

    /**
     * Test: Get groups by lab ID - lab not found
     */
    @Test
    void testGetGroupsByLabId_LabNotFound() {
        // Arrange
        when(labService.labExists("lab1")).thenReturn(false);

        // Act
        ResponseEntity<?> response = labController.getGroupsByLabId("lab1");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Lab with ID lab1 not found"));
    }
}
