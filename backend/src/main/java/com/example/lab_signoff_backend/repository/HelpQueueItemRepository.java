package com.example.lab_signoff_backend.repository;

import com.example.lab_signoff_backend.model.HelpQueueItem;
import com.example.lab_signoff_backend.model.enums.HelpQueueStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for HelpQueueItem entity
 * Manages the per-lab help queue (hands raised) system
 */
@Repository
public interface HelpQueueItemRepository extends MongoRepository<HelpQueueItem, String> {

    /**
     * Find all queue items for a lab with a specific status, ordered by position
     *
     * @param labId  The lab ID
     * @param status The queue item status
     * @return List of queue items ordered by position (ascending)
     */
    List<HelpQueueItem> findByLabIdAndStatusOrderByPositionAsc(String labId, HelpQueueStatus status);

    /**
     * Find all queue items for a lab (all statuses)
     *
     * @param labId The lab ID
     * @return List of all queue items for the lab
     */
    List<HelpQueueItem> findByLabId(String labId);

    /**
     * Find all queue items claimed by a specific TA/teacher
     *
     * @param userId The TA/teacher's user ID
     * @return List of queue items claimed by this user
     */
    List<HelpQueueItem> findByClaimedBy(String userId);

    /**
     * Find all queue items raised by a specific student
     *
     * @param userId The student's user ID
     * @return List of queue items raised by this user
     */
    List<HelpQueueItem> findByRaisedBy(String userId);

    /**
     * Find an active (waiting or claimed) queue item for a specific group in a lab
     *
     * @param labId   The lab ID
     * @param groupId The group ID
     * @param status  The queue status
     * @return Optional containing the queue item if found
     */
    Optional<HelpQueueItem> findByLabIdAndGroupIdAndStatus(String labId, String groupId, HelpQueueStatus status);

    /**
     * Find all waiting queue items for a lab, ordered by position
     *
     * @param labId The lab ID
     * @return List of waiting queue items
     */
    default List<HelpQueueItem> findWaitingByLab(String labId) {
        return findByLabIdAndStatusOrderByPositionAsc(labId, HelpQueueStatus.WAITING);
    }

    /**
     * Find all claimed queue items for a lab
     *
     * @param labId The lab ID
     * @return List of claimed queue items
     */
    default List<HelpQueueItem> findClaimedByLab(String labId) {
        return findByLabIdAndStatusOrderByPositionAsc(labId, HelpQueueStatus.CLAIMED);
    }

    /**
     * Find all active (waiting or claimed) queue items for a lab
     *
     * @param labId The lab ID
     * @return List of active queue items
     */
    List<HelpQueueItem> findByLabIdAndStatusIn(String labId, List<HelpQueueStatus> statuses);

    /**
     * Count queue items in a specific status for a lab
     *
     * @param labId  The lab ID
     * @param status The queue status
     * @return Count of queue items
     */
    long countByLabIdAndStatus(String labId, HelpQueueStatus status);

    /**
     * Find the highest position number for a lab (for adding new items)
     *
     * @param labId The lab ID
     * @return The queue item with the highest position, or empty if no items
     */
    Optional<HelpQueueItem> findFirstByLabIdOrderByPositionDesc(String labId);
}
