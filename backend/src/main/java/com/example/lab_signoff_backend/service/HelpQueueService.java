package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.HelpQueueItem;
import com.example.lab_signoff_backend.model.enums.HelpQueueStatus;
import com.example.lab_signoff_backend.repository.HelpQueueItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service class for Help Queue management operations
 */
@Service
public class HelpQueueService {

    @Autowired
    private HelpQueueItemRepository helpQueueItemRepository;

    /**
     * Raise hand - Add a new help request to the queue
     */
    public HelpQueueItem raiseHand(String labId, String groupId, String raisedBy, String description) {
        // Check if group already has an active request
        List<HelpQueueStatus> activeStatuses = Arrays.asList(
                HelpQueueStatus.WAITING,
                HelpQueueStatus.CLAIMED
        );

        List<HelpQueueItem> existingRequests = helpQueueItemRepository.findByLabIdAndStatusIn(labId, activeStatuses);
        boolean groupHasActiveRequest = existingRequests.stream()
                .anyMatch(item -> item.getGroupId().equals(groupId));

        if (groupHasActiveRequest) {
            throw new RuntimeException("Group already has an active help request");
        }

        // Calculate next position in queue
        int nextPosition = getNextPosition(labId);

        // Create new queue item
        HelpQueueItem queueItem = new HelpQueueItem(labId, groupId, raisedBy, nextPosition);
        if (description != null && !description.trim().isEmpty()) {
            queueItem.setDescription(description);
        }

        return helpQueueItemRepository.save(queueItem);
    }

    /**
     * Claim a help request (TA/Teacher takes ownership)
     */
    public HelpQueueItem claimRequest(String queueItemId, String userId) {
        Optional<HelpQueueItem> queueItemOpt = helpQueueItemRepository.findById(queueItemId);
        if (queueItemOpt.isEmpty()) {
            throw new RuntimeException("Queue item not found with id: " + queueItemId);
        }

        HelpQueueItem queueItem = queueItemOpt.get();

        if (!queueItem.isWaiting()) {
            throw new RuntimeException("Can only claim waiting requests. Current status: " + queueItem.getStatus());
        }

        queueItem.claim(userId);
        return helpQueueItemRepository.save(queueItem);
    }

    /**
     * Resolve a help request (mark as completed)
     */
    public HelpQueueItem resolveRequest(String queueItemId) {
        Optional<HelpQueueItem> queueItemOpt = helpQueueItemRepository.findById(queueItemId);
        if (queueItemOpt.isEmpty()) {
            throw new RuntimeException("Queue item not found with id: " + queueItemId);
        }

        HelpQueueItem queueItem = queueItemOpt.get();

        if (!queueItem.isClaimed()) {
            throw new RuntimeException("Can only resolve claimed requests. Current status: " + queueItem.getStatus());
        }

        queueItem.resolve();
        return helpQueueItemRepository.save(queueItem);
    }

    /**
     * Cancel a help request (student cancels)
     */
    public HelpQueueItem cancelRequest(String queueItemId) {
        Optional<HelpQueueItem> queueItemOpt = helpQueueItemRepository.findById(queueItemId);
        if (queueItemOpt.isEmpty()) {
            throw new RuntimeException("Queue item not found with id: " + queueItemId);
        }

        HelpQueueItem queueItem = queueItemOpt.get();

        if (!queueItem.isActive()) {
            throw new RuntimeException("Can only cancel active requests");
        }

        queueItem.cancel();
        return helpQueueItemRepository.save(queueItem);
    }

    /**
     * Get all queue items for a lab (all statuses)
     */
    public List<HelpQueueItem> getQueueForLab(String labId) {
        return helpQueueItemRepository.findByLabId(labId);
    }

    /**
     * Get waiting queue items for a lab (ordered by position)
     */
    public List<HelpQueueItem> getWaitingQueue(String labId) {
        return helpQueueItemRepository.findWaitingByLab(labId);
    }

    /**
     * Get claimed queue items for a lab
     */
    public List<HelpQueueItem> getClaimedQueue(String labId) {
        return helpQueueItemRepository.findClaimedByLab(labId);
    }

    /**
     * Get active (waiting or claimed) queue items for a lab
     */
    public List<HelpQueueItem> getActiveQueue(String labId) {
        List<HelpQueueStatus> activeStatuses = Arrays.asList(
                HelpQueueStatus.WAITING,
                HelpQueueStatus.CLAIMED
        );
        return helpQueueItemRepository.findByLabIdAndStatusIn(labId, activeStatuses);
    }

    /**
     * Get queue items claimed by a specific TA
     */
    public List<HelpQueueItem> getClaimedByUser(String userId) {
        return helpQueueItemRepository.findByClaimedBy(userId);
    }

    /**
     * Get queue items raised by a specific student
     */
    public List<HelpQueueItem> getRaisedByUser(String userId) {
        return helpQueueItemRepository.findByRaisedBy(userId);
    }

    /**
     * Get a specific queue item by ID
     */
    public Optional<HelpQueueItem> getQueueItem(String id) {
        return helpQueueItemRepository.findById(id);
    }

    /**
     * Delete a queue item
     */
    public void deleteQueueItem(String id) {
        helpQueueItemRepository.deleteById(id);
    }

    /**
     * Set a queue item as urgent
     */
    public HelpQueueItem setUrgent(String queueItemId) {
        Optional<HelpQueueItem> queueItemOpt = helpQueueItemRepository.findById(queueItemId);
        if (queueItemOpt.isEmpty()) {
            throw new RuntimeException("Queue item not found with id: " + queueItemId);
        }

        HelpQueueItem queueItem = queueItemOpt.get();
        queueItem.setUrgent();
        return helpQueueItemRepository.save(queueItem);
    }

    /**
     * Count active queue items for a lab
     */
    public long countActiveItems(String labId) {
        long waiting = helpQueueItemRepository.countByLabIdAndStatus(labId, HelpQueueStatus.WAITING);
        long claimed = helpQueueItemRepository.countByLabIdAndStatus(labId, HelpQueueStatus.CLAIMED);
        return waiting + claimed;
    }

    /**
     * Count waiting queue items for a lab
     */
    public long countWaitingItems(String labId) {
        return helpQueueItemRepository.countByLabIdAndStatus(labId, HelpQueueStatus.WAITING);
    }

    /**
     * Get the next position number for the queue
     */
    private int getNextPosition(String labId) {
        Optional<HelpQueueItem> lastItem = helpQueueItemRepository.findFirstByLabIdOrderByPositionDesc(labId);
        return lastItem.map(item -> item.getPosition() + 1).orElse(1);
    }

    /**
     * Check if a group has an active help request
     */
    public boolean hasActiveRequest(String labId, String groupId) {
        List<HelpQueueStatus> activeStatuses = Arrays.asList(
                HelpQueueStatus.WAITING,
                HelpQueueStatus.CLAIMED
        );

        List<HelpQueueItem> items = helpQueueItemRepository.findByLabIdAndStatusIn(labId, activeStatuses);
        return items.stream().anyMatch(item -> item.getGroupId().equals(groupId));
    }

    /**
     * Get active request for a group (if exists)
     */
    public Optional<HelpQueueItem> getActiveRequestForGroup(String labId, String groupId) {
        // Try waiting first
        Optional<HelpQueueItem> waiting = helpQueueItemRepository
                .findByLabIdAndGroupIdAndStatus(labId, groupId, HelpQueueStatus.WAITING);
        if (waiting.isPresent()) {
            return waiting;
        }

        // Try claimed
        return helpQueueItemRepository
                .findByLabIdAndGroupIdAndStatus(labId, groupId, HelpQueueStatus.CLAIMED);
    }

    /**
     * Clear all resolved/cancelled items for a lab (cleanup)
     */
    public void clearClosedItems(String labId) {
        List<HelpQueueItem> allItems = helpQueueItemRepository.findByLabId(labId);
        allItems.stream()
                .filter(item -> item.isResolved() || item.isCancelled())
                .forEach(item -> helpQueueItemRepository.delete(item));
    }
}
