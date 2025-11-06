package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.SignoffEvent;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.repository.SignoffEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service class for SignoffEvent entity business logic.
 *
 * Provides methods for creating audit entries, retrieving signoff event history,
 * and managing signoff event operations.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@Service
public class SignoffEventService {

    private final SignoffEventRepository repository;

    /**
     * Constructor for SignoffEventService.
     *
     * @param repository The SignoffEventRepository for database operations
     */
    public SignoffEventService(SignoffEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Create a new signoff event audit entry
     *
     * @param signoffEvent The signoff event to create
     * @return The created signoff event with generated ID
     */
    public SignoffEvent createEvent(SignoffEvent signoffEvent) {
        if (signoffEvent.getTimestamp() == null) {
            signoffEvent.setTimestamp(Instant.now());
        }
        return repository.save(signoffEvent);
    }

    /**
     * Create a signoff event with basic information
     *
     * @param labId       The lab identifier
     * @param groupId     The group identifier
     * @param action      The action (PASS or RETURN)
     * @param performedBy Who performed the action
     * @return The created signoff event
     */
    public SignoffEvent createEvent(String labId, String groupId, String action, String performedBy) {
        SignoffEvent event = new SignoffEvent();
        event.setLabId(labId);
        event.setGroupId(groupId);
        event.setAction(SignoffAction.valueOf(action));
        event.setPerformedBy(performedBy);
        return repository.save(event);
    }

    /**
     * Create a signoff event with additional details
     *
     * @param labId            The lab identifier
     * @param groupId          The group identifier
     * @param action           The action (PASS or RETURN)
     * @param performedBy      Who performed the action
     * @param notes            Optional notes about the action
     * @param checkpointNumber Optional checkpoint number
     * @return The created signoff event
     */
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

    /**
     * Retrieve all signoff events for a specific lab
     *
     * @param labId The lab identifier
     * @return List of signoff events for the lab
     */
    public List<SignoffEvent> getEventsByLabId(String labId) {
        return repository.findByLabId(labId);
    }

    /**
     * Retrieve all signoff events for a specific group
     *
     * @param groupId The group identifier
     * @return List of signoff events for the group
     */
    public List<SignoffEvent> getEventsByGroupId(String groupId) {
        return repository.findByGroupId(groupId);
    }

    /**
     * Retrieve all signoff events for a specific lab and group
     *
     * @param labId   The lab identifier
     * @param groupId The group identifier
     * @return List of signoff events for the lab and group
     */
    public List<SignoffEvent> getEventsByLabIdAndGroupId(String labId, String groupId) {
        return repository.findByLabIdAndGroupId(labId, groupId);
    }

    /**
     * Retrieve all signoff events performed by a specific user
     *
     * @param performedBy The user identifier
     * @return List of signoff events performed by the user
     */
    public List<SignoffEvent> getEventsByPerformedBy(String performedBy) {
        return repository.findByPerformedBy(performedBy);
    }

    /**
     * Retrieve all signoff events within a time range
     *
     * @param start Start of the time range
     * @param end   End of the time range
     * @return List of signoff events within the time range
     */
    public List<SignoffEvent> getEventsByTimeRange(Instant start, Instant end) {
        return repository.findByTimestampBetween(start, end);
    }

    /**
     * Retrieve all signoff events of a specific action type
     *
     * @param action The action type (PASS or RETURN)
     * @return List of signoff events with the specified action
     */
    public List<SignoffEvent> getEventsByAction(String action) {
        return repository.findByAction(action);
    }

    /**
     * Retrieve all signoff events
     *
     * @return List of all signoff events
     */
    public List<SignoffEvent> getAllEvents() {
        return repository.findAll();
    }

    /**
     * Retrieve a specific signoff event by ID
     *
     * @param id The signoff event identifier
     * @return Optional containing the signoff event if found
     */
    public Optional<SignoffEvent> getEventById(String id) {
        return repository.findById(id);
    }

    /**
     * Delete a signoff event by ID
     *
     * @param id The signoff event identifier
     */
    public void deleteEvent(String id) {
        repository.deleteById(id);
    }

    /**
     * Count all signoff events for a specific lab
     *
     * @param labId The lab identifier
     * @return Number of signoff events for the lab
     */
    public long countEventsByLabId(String labId) {
        return repository.findByLabId(labId).size();
    }
}
