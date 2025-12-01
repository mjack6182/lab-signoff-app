
/**
 * Service class for managing checkpoint signoffs and audit events.
 *
 * This class handles creating, retrieving, and deleting SignoffEvent entries in MongoDB.
 * It ensures that every checkpoint signoff and group action is persisted before any
 * WebSocket broadcast occurs, so that progress is not lost on page reloads.
 */

package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.SignoffEvent;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.repository.SignoffEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SignoffEventService {

    private final SignoffEventRepository repository;

    // Constructor injection of repository
    public SignoffEventService(SignoffEventRepository repository) {
        this.repository = repository;
    }

    // Save a new signoff event
    public SignoffEvent createEvent(SignoffEvent signoffEvent) {
        if (signoffEvent.getTimestamp() == null) {
            signoffEvent.setTimestamp(Instant.now()); // set current time if missing
        }
        return repository.save(signoffEvent); // persist to MongoDB
    }

    // Convenience method with basic info
    public SignoffEvent createEvent(String labId, String groupId, String action, String performedBy) {
        SignoffEvent event = new SignoffEvent();
        event.setLabId(labId);
        event.setGroupId(groupId);
        event.setAction(SignoffAction.valueOf(action));
        event.setPerformedBy(performedBy);
        return repository.save(event);
    }

    // Extended creation method with notes and checkpoint number
    public SignoffEvent createEvent(String labId, String groupId, String action,
                                   String performedBy, String notes, Integer checkpointNumber) {
        SignoffEvent event = new SignoffEvent();
        event.setLabId(labId);
        event.setGroupId(groupId);
        event.setAction(SignoffAction.valueOf(action));
        event.setPerformedBy(performedBy);
        event.setNotes(notes);
        event.setCheckpointNumber(checkpointNumber);
        return repository.save(event);
    }

    // Get all events for a lab
    public List<SignoffEvent> getEventsByLabId(String labId) {
        return repository.findByLabId(labId);
    }

    // Get all events for a group
    public List<SignoffEvent> getEventsByGroupId(String groupId) {
        return repository.findByGroupId(groupId);
    }

    // Get events for a specific lab and group
    public List<SignoffEvent> getEventsByLabIdAndGroupId(String labId, String groupId) {
        return repository.findByLabIdAndGroupId(labId, groupId);
    }

    // Get events performed by a specific user
    public List<SignoffEvent> getEventsByPerformedBy(String performedBy) {
        return repository.findByPerformedBy(performedBy);
    }

    // Get events within a time range
    public List<SignoffEvent> getEventsByTimeRange(Instant start, Instant end) {
        return repository.findByTimestampBetween(start, end);
    }

    // Get events by action type (PASS or RETURN)
    public List<SignoffEvent> getEventsByAction(String action) {
        return repository.findByAction(action);
    }

    // Get all signoff events
    public List<SignoffEvent> getAllEvents() {
        return repository.findAll();
    }

    // Get event by ID
    public Optional<SignoffEvent> getEventById(String id) {
        return repository.findById(id);
    }

    // Delete event by ID
    public void deleteEvent(String id) {
        repository.deleteById(id);
    }

    // Count events for a lab
    public long countEventsByLabId(String labId) {
        return repository.findByLabId(labId).size();
    }
}