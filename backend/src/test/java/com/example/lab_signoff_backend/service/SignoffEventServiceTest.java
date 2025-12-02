package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.SignoffEvent;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.repository.SignoffEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SignoffEventService
 *
 * Tests the service layer logic for managing signoff events,
 * including creation, retrieval, and query operations.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class SignoffEventServiceTest {

    @Mock
    private SignoffEventRepository repository;

    @InjectMocks
    private SignoffEventService service;

    private SignoffEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new SignoffEvent();
        testEvent.setId("event1");
        testEvent.setLabId("lab1");
        testEvent.setGroupId("group1");
        testEvent.setAction(SignoffAction.PASS);
        testEvent.setTimestamp(Instant.now());
        testEvent.setPerformedBy("instructor1");
        testEvent.setNotes("Great work!");
    }

    /**
     * Test: Create event with full SignoffEvent object
     */
    @Test
    void testCreateEvent_WithObject() {
        // Arrange
        SignoffEvent newEvent = new SignoffEvent();
        newEvent.setLabId("lab2");
        newEvent.setGroupId("group2");
        newEvent.setAction(SignoffAction.RETURN);
        newEvent.setPerformedBy("instructor2");

        when(repository.save(any(SignoffEvent.class))).thenReturn(testEvent);

        // Act
        SignoffEvent result = service.createEvent(newEvent);

        // Assert
        assertNotNull(result);
        verify(repository).save(newEvent);
    }

    /**
     * Test: Create event automatically sets timestamp if null
     */
    @Test
    void testCreateEvent_AutoSetTimestamp() {
        // Arrange
        SignoffEvent eventWithoutTimestamp = new SignoffEvent();
        eventWithoutTimestamp.setLabId("lab1");
        eventWithoutTimestamp.setGroupId("group1");
        eventWithoutTimestamp.setAction(SignoffAction.PASS);
        eventWithoutTimestamp.setPerformedBy("instructor1");
        // No timestamp set

        when(repository.save(any(SignoffEvent.class))).thenReturn(eventWithoutTimestamp);

        // Act
        service.createEvent(eventWithoutTimestamp);

        // Assert
        ArgumentCaptor<SignoffEvent> captor = ArgumentCaptor.forClass(SignoffEvent.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue().getTimestamp());
    }

    @Test
    void createEvent_doesNotOverrideExistingTimestamp() {
        Instant ts = Instant.now().minusSeconds(60);
        SignoffEvent event = new SignoffEvent();
        event.setLabId("lab1");
        event.setGroupId("group1");
        event.setTimestamp(ts);

        when(repository.save(any(SignoffEvent.class))).thenReturn(event);

        SignoffEvent saved = service.createEvent(event);

        assertEquals(ts, saved.getTimestamp());
        verify(repository).save(event);
    }

    /**
     * Test: Create event with basic information
     */
    @Test
    void testCreateEvent_WithBasicInfo() {
        // Arrange
        when(repository.save(any(SignoffEvent.class))).thenReturn(testEvent);

        // Act
        SignoffEvent result = service.createEvent("lab1", "group1", "PASS", "instructor1");

        // Assert
        assertNotNull(result);
        ArgumentCaptor<SignoffEvent> captor = ArgumentCaptor.forClass(SignoffEvent.class);
        verify(repository).save(captor.capture());

        SignoffEvent savedEvent = captor.getValue();
        assertEquals("lab1", savedEvent.getLabId());
        assertEquals("group1", savedEvent.getGroupId());
        assertEquals(SignoffAction.PASS, savedEvent.getAction());
        assertEquals("instructor1", savedEvent.getPerformedBy());
        assertNotNull(savedEvent.getTimestamp());
    }

    /**
     * Test: Create event with additional details
     */
    @Test
    void testCreateEvent_WithAdditionalDetails() {
        // Arrange
        when(repository.save(any(SignoffEvent.class))).thenReturn(testEvent);

        // Act
        SignoffEvent result = service.createEvent(
                "lab1", "group1", "RETURN", "instructor1", "Needs revision", 2);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<SignoffEvent> captor = ArgumentCaptor.forClass(SignoffEvent.class);
        verify(repository).save(captor.capture());

        SignoffEvent savedEvent = captor.getValue();
        assertEquals("lab1", savedEvent.getLabId());
        assertEquals("group1", savedEvent.getGroupId());
        assertEquals(SignoffAction.RETURN, savedEvent.getAction());
        assertEquals("instructor1", savedEvent.getPerformedBy());
        assertEquals("Needs revision", savedEvent.getNotes());
        assertEquals(2, savedEvent.getCheckpointNumber());
        assertNotNull(savedEvent.getTimestamp());
    }

    /**
     * Test: Get events by lab ID
     */
    @Test
    void testGetEventsByLabId() {
        // Arrange
        List<SignoffEvent> events = Arrays.asList(testEvent);
        when(repository.findByLabId("lab1")).thenReturn(events);

        // Act
        List<SignoffEvent> result = service.getEventsByLabId("lab1");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("lab1", result.get(0).getLabId());
        verify(repository).findByLabId("lab1");
    }

    /**
     * Test: Get events by group ID
     */
    @Test
    void testGetEventsByGroupId() {
        // Arrange
        List<SignoffEvent> events = Arrays.asList(testEvent);
        when(repository.findByGroupId("group1")).thenReturn(events);

        // Act
        List<SignoffEvent> result = service.getEventsByGroupId("group1");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("group1", result.get(0).getGroupId());
        verify(repository).findByGroupId("group1");
    }

    /**
     * Test: Get events by lab ID and group ID
     */
    @Test
    void testGetEventsByLabIdAndGroupId() {
        // Arrange
        List<SignoffEvent> events = Arrays.asList(testEvent);
        when(repository.findByLabIdAndGroupId("lab1", "group1")).thenReturn(events);

        // Act
        List<SignoffEvent> result = service.getEventsByLabIdAndGroupId("lab1", "group1");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("lab1", result.get(0).getLabId());
        assertEquals("group1", result.get(0).getGroupId());
        verify(repository).findByLabIdAndGroupId("lab1", "group1");
    }

    /**
     * Test: Get events by performed by user
     */
    @Test
    void testGetEventsByPerformedBy() {
        // Arrange
        List<SignoffEvent> events = Arrays.asList(testEvent);
        when(repository.findByPerformedBy("instructor1")).thenReturn(events);

        // Act
        List<SignoffEvent> result = service.getEventsByPerformedBy("instructor1");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("instructor1", result.get(0).getPerformedBy());
        verify(repository).findByPerformedBy("instructor1");
    }

    /**
     * Test: Get events by time range
     */
    @Test
    void testGetEventsByTimeRange() {
        // Arrange
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();
        List<SignoffEvent> events = Arrays.asList(testEvent);
        when(repository.findByTimestampBetween(start, end)).thenReturn(events);

        // Act
        List<SignoffEvent> result = service.getEventsByTimeRange(start, end);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByTimestampBetween(start, end);
    }

    /**
     * Test: Get events by action
     */
    @Test
    void testGetEventsByAction() {
        // Arrange
        List<SignoffEvent> events = Arrays.asList(testEvent);
        when(repository.findByAction("PASS")).thenReturn(events);

        // Act
        List<SignoffEvent> result = service.getEventsByAction("PASS");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(SignoffAction.PASS, result.get(0).getAction());
        verify(repository).findByAction("PASS");
    }

    /**
     * Test: Get all events
     */
    @Test
    void testGetAllEvents() {
        // Arrange
        List<SignoffEvent> events = Arrays.asList(testEvent);
        when(repository.findAll()).thenReturn(events);

        // Act
        List<SignoffEvent> result = service.getAllEvents();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findAll();
    }

    /**
     * Test: Get event by ID
     */
    @Test
    void testGetEventById() {
        // Arrange
        when(repository.findById("event1")).thenReturn(Optional.of(testEvent));

        // Act
        Optional<SignoffEvent> result = service.getEventById("event1");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("event1", result.get().getId());
        verify(repository).findById("event1");
    }

    /**
     * Test: Get event by ID - not found
     */
    @Test
    void testGetEventById_NotFound() {
        // Arrange
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<SignoffEvent> result = service.getEventById("nonexistent");

        // Assert
        assertFalse(result.isPresent());
        verify(repository).findById("nonexistent");
    }

    /**
     * Test: Delete event
     */
    @Test
    void testDeleteEvent() {
        // Act
        service.deleteEvent("event1");

        // Assert
        verify(repository).deleteById("event1");
    }

    /**
     * Test: Count events by lab ID
     */
    @Test
    void testCountEventsByLabId() {
        // Arrange
        List<SignoffEvent> events = Arrays.asList(testEvent, testEvent, testEvent);
        when(repository.findByLabId("lab1")).thenReturn(events);

        // Act
        long count = service.countEventsByLabId("lab1");

        // Assert
        assertEquals(3, count);
        verify(repository).findByLabId("lab1");
    }

    /**
     * Test: Count events by lab ID - no events
     */
    @Test
    void testCountEventsByLabId_NoEvents() {
        // Arrange
        when(repository.findByLabId("lab1")).thenReturn(Arrays.asList());

        // Act
        long count = service.countEventsByLabId("lab1");

        // Assert
        assertEquals(0, count);
        verify(repository).findByLabId("lab1");
    }

    @Test
    void testCreateEvent_keepsExistingTimestamp() {
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");
        SignoffEvent event = new SignoffEvent();
        event.setTimestamp(timestamp);
        when(repository.save(any(SignoffEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        SignoffEvent saved = service.createEvent(event);

        assertEquals(timestamp, saved.getTimestamp());
        verify(repository).save(event);
    }
}
