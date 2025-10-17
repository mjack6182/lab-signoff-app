package com.example.lab_signoff_backend.repository;

import com.example.lab_signoff_backend.model.SignoffEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for SignoffEvent entity database operations.
 *
 * Extends MongoRepository to provide CRUD operations and custom query methods
 * for the signoff_events collection.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@Repository
public interface SignoffEventRepository extends MongoRepository<SignoffEvent, String> {

    /**
     * Find all signoff events for a specific lab
     *
     * @param labId The lab identifier
     * @return List of signoff events for the lab
     */
    List<SignoffEvent> findByLabId(String labId);

    /**
     * Find all signoff events for a specific group
     *
     * @param groupId The group identifier
     * @return List of signoff events for the group
     */
    List<SignoffEvent> findByGroupId(String groupId);

    /**
     * Find all signoff events for a specific lab and group combination
     *
     * @param labId   The lab identifier
     * @param groupId The group identifier
     * @return List of signoff events for the lab and group
     */
    List<SignoffEvent> findByLabIdAndGroupId(String labId, String groupId);

    /**
     * Find all signoff events performed by a specific user
     *
     * @param performedBy The user identifier who performed the actions
     * @return List of signoff events performed by the user
     */
    List<SignoffEvent> findByPerformedBy(String performedBy);

    /**
     * Find all signoff events within a time range
     *
     * @param start Start of the time range
     * @param end   End of the time range
     * @return List of signoff events within the time range
     */
    List<SignoffEvent> findByTimestampBetween(Instant start, Instant end);

    /**
     * Find all signoff events for a specific action type (PASS or RETURN)
     *
     * @param action The action type
     * @return List of signoff events with the specified action
     */
    List<SignoffEvent> findByAction(String action);
}
