package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.HelpQueueItem;
import com.example.lab_signoff_backend.service.HelpQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Help Queue management
 * Handles the per-lab help queue (hands raised) system
 */
@RestController
@RequestMapping("/api/queue")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class HelpQueueController {

    @Autowired
    private HelpQueueService helpQueueService;

    /**
     * Raise hand - Add a new help request to the queue
     * POST /api/labs/{labId}/queue
     * Body: { "groupId": "xyz", "raisedBy": "userId", "description": "Need help with..." }
     */
    @PostMapping("/labs/{labId}")
    public ResponseEntity<HelpQueueItem> raiseHand(
            @PathVariable String labId,
            @RequestBody Map<String, String> request) {
        try {
            String groupId = request.get("groupId");
            String raisedBy = request.get("raisedBy");
            String description = request.get("description");

            if (groupId == null || raisedBy == null) {
                return ResponseEntity.badRequest().build();
            }

            HelpQueueItem queueItem = helpQueueService.raiseHand(labId, groupId, raisedBy, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(queueItem);
        } catch (RuntimeException e) {
            // Group already has active request
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get queue for a lab
     * GET /api/labs/{labId}/queue?status=waiting
     */
    @GetMapping("/labs/{labId}")
    public ResponseEntity<List<HelpQueueItem>> getQueueForLab(
            @PathVariable String labId,
            @RequestParam(required = false) String status) {
        try {
            List<HelpQueueItem> queue;

            if (status == null || status.isEmpty()) {
                queue = helpQueueService.getActiveQueue(labId);
            } else if (status.equalsIgnoreCase("waiting")) {
                queue = helpQueueService.getWaitingQueue(labId);
            } else if (status.equalsIgnoreCase("claimed")) {
                queue = helpQueueService.getClaimedQueue(labId);
            } else {
                queue = helpQueueService.getQueueForLab(labId);
            }

            return ResponseEntity.ok(queue);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific queue item
     * GET /api/queue/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<HelpQueueItem> getQueueItem(@PathVariable String id) {
        Optional<HelpQueueItem> queueItem = helpQueueService.getQueueItem(id);
        return queueItem.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Claim a help request
     * PUT /api/queue/{id}/claim
     * Body: { "userId": "taUserId" }
     */
    @PutMapping("/{id}/claim")
    public ResponseEntity<HelpQueueItem> claimRequest(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            HelpQueueItem updated = helpQueueService.claimRequest(id, userId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Resolve a help request
     * PUT /api/queue/{id}/resolve
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<HelpQueueItem> resolveRequest(@PathVariable String id) {
        try {
            HelpQueueItem updated = helpQueueService.resolveRequest(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cancel a help request
     * DELETE /api/queue/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<HelpQueueItem> cancelRequest(@PathVariable String id) {
        try {
            HelpQueueItem updated = helpQueueService.cancelRequest(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Set queue item as urgent
     * PUT /api/queue/{id}/urgent
     */
    @PutMapping("/{id}/urgent")
    public ResponseEntity<HelpQueueItem> setUrgent(@PathVariable String id) {
        try {
            HelpQueueItem updated = helpQueueService.setUrgent(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get queue items claimed by a specific TA
     * GET /api/queue/user/{userId}/claimed
     */
    @GetMapping("/user/{userId}/claimed")
    public ResponseEntity<List<HelpQueueItem>> getClaimedByUser(@PathVariable String userId) {
        try {
            List<HelpQueueItem> items = helpQueueService.getClaimedByUser(userId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get queue items raised by a specific student
     * GET /api/queue/user/{userId}/raised
     */
    @GetMapping("/user/{userId}/raised")
    public ResponseEntity<List<HelpQueueItem>> getRaisedByUser(@PathVariable String userId) {
        try {
            List<HelpQueueItem> items = helpQueueService.getRaisedByUser(userId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get queue statistics for a lab
     * GET /api/labs/{labId}/queue/stats
     */
    @GetMapping("/labs/{labId}/stats")
    public ResponseEntity<Map<String, Long>> getQueueStats(@PathVariable String labId) {
        try {
            long waiting = helpQueueService.countWaitingItems(labId);
            long active = helpQueueService.countActiveItems(labId);

            return ResponseEntity.ok(Map.of(
                    "waiting", waiting,
                    "active", active,
                    "claimed", active - waiting
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if a group has an active help request
     * GET /api/labs/{labId}/groups/{groupId}/queue/check
     */
    @GetMapping("/labs/{labId}/groups/{groupId}/check")
    public ResponseEntity<Map<String, Object>> checkActiveRequest(
            @PathVariable String labId,
            @PathVariable String groupId) {
        boolean hasActive = helpQueueService.hasActiveRequest(labId, groupId);
        Optional<HelpQueueItem> activeRequest = helpQueueService.getActiveRequestForGroup(labId, groupId);

        return ResponseEntity.ok(Map.of(
                "hasActiveRequest", hasActive,
                "activeRequest", activeRequest.orElse(null)
        ));
    }

    /**
     * Clear closed items (resolved/cancelled) for a lab
     * DELETE /api/labs/{labId}/queue/cleanup
     */
    @DeleteMapping("/labs/{labId}/cleanup")
    public ResponseEntity<Void> clearClosedItems(@PathVariable String labId) {
        try {
            helpQueueService.clearClosedItems(labId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
