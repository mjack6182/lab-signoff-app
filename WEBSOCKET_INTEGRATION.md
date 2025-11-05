# WebSocket Integration Guide

## Overview

The Lab Signoff App uses WebSockets (STOMP over SockJS) for real-time communication between the backend and frontend. This document describes the updated WebSocket implementation that works with the new database schema.

## Architecture

### Backend (Spring Boot + STOMP)

- **Configuration**: `WebSocketConfig.java`
- **Controller**: `LabWebSocketController.java`
- **Message DTOs**: `CheckpointUpdate.java`, `GroupStatusUpdate.java`, `HelpQueueUpdate.java`

### Frontend (TypeScript + @stomp/stompjs)

- **Service**: `websocketServiceEnhanced.ts`
- **Types**: `types/websocket.ts`
- **Connection**: SockJS with STOMP protocol

---

## WebSocket Topics

### Lab-Specific Topics

Subscribe to these topics to receive updates for a specific lab:

```
/topic/labs/{labId}/checkpoints    - Checkpoint updates for all groups in the lab
/topic/labs/{labId}/groups          - Group status changes in the lab
/topic/labs/{labId}/help-queue      - Help queue updates for the lab
```

### Group-Specific Topics

Subscribe to these topics to receive updates for a specific group:

```
/topic/groups/{groupId}/checkpoints   - Checkpoint updates for this group only
/topic/groups/{groupId}/status        - Status changes for this group
/topic/groups/{groupId}/help-queue    - Help queue updates for this group
```

### Legacy Topics (Backward Compatibility)

```
/topic/group-updates                  - Old format, deprecated
```

---

## Message Types

### 1. CheckpointUpdate

**Sent when:** A checkpoint is signed off (PASS) or returned (RETURN)

**Backend DTO:**
```java
public class CheckpointUpdate {
    private String labId;
    private String groupId;
    private Integer checkpointNumber;
    private String status; // "PASS" or "RETURN"
    private String signedOffBy;
    private String signedOffByName;
    private Instant timestamp;
    private String notes;
    private Integer pointsAwarded;
}
```

**Frontend Type:**
```typescript
interface CheckpointUpdate {
  labId: string;
  groupId: string;
  checkpointNumber: number;
  status: 'PASS' | 'RETURN';
  signedOffBy?: string;
  signedOffByName?: string;
  timestamp: string; // ISO 8601
  notes?: string;
  pointsAwarded?: number;
}
```

**Example Message:**
```json
{
  "labId": "673a1b2c3d4e5f6789abcdef",
  "groupId": "group-alpha-001",
  "checkpointNumber": 3,
  "status": "PASS",
  "signedOffBy": "user-teacher-123",
  "signedOffByName": "Dr. Sarah Johnson",
  "timestamp": "2025-11-04T23:45:12.123Z",
  "notes": "Great work on error handling!",
  "pointsAwarded": 1
}
```

---

### 2. GroupStatusUpdate

**Sent when:** A group's overall status changes (e.g., FORMING → IN_PROGRESS → SIGNED_OFF)

**Backend DTO:**
```java
public class GroupStatusUpdate {
    private String labId;
    private String groupId;
    private GroupStatus status; // FORMING, IN_PROGRESS, COMPLETED, SIGNED_OFF
    private GroupStatus previousStatus;
    private Instant timestamp;
    private String performedBy;
    private String performedByName;
    private Integer totalScore;
    private String finalGrade;
}
```

**Frontend Type:**
```typescript
interface GroupStatusUpdate {
  labId: string;
  groupId: string;
  status: 'FORMING' | 'IN_PROGRESS' | 'COMPLETED' | 'SIGNED_OFF';
  previousStatus?: GroupStatus;
  timestamp: string;
  performedBy?: string;
  performedByName?: string;
  totalScore?: number;
  finalGrade?: string;
}
```

**Example Message:**
```json
{
  "labId": "673a1b2c3d4e5f6789abcdef",
  "groupId": "group-alpha-001",
  "status": "SIGNED_OFF",
  "previousStatus": "COMPLETED",
  "timestamp": "2025-11-04T23:50:00.000Z",
  "performedBy": "user-teacher-123",
  "performedByName": "Dr. Sarah Johnson",
  "totalScore": 4,
  "finalGrade": "A"
}
```

---

### 3. HelpQueueUpdate

**Sent when:** Help requests are raised, claimed, or resolved

**Backend DTO:**
```java
public class HelpQueueUpdate {
    private String id;
    private String labId;
    private String groupId;
    private HelpQueueStatus status; // WAITING, CLAIMED, RESOLVED, CANCELLED
    private HelpQueueStatus previousStatus;
    private HelpQueuePriority priority; // NORMAL, URGENT
    private Integer position;
    private String requestedBy;
    private String requestedByName;
    private String claimedBy;
    private String claimedByName;
    private Instant timestamp;
    private String description;
}
```

**Frontend Type:**
```typescript
interface HelpQueueUpdate {
  id: string;
  labId: string;
  groupId: string;
  status: 'WAITING' | 'CLAIMED' | 'RESOLVED' | 'CANCELLED';
  previousStatus?: HelpQueueStatus;
  priority: 'NORMAL' | 'URGENT';
  position?: number;
  requestedBy: string;
  requestedByName?: string;
  claimedBy?: string;
  claimedByName?: string;
  timestamp: string;
  description?: string;
}
```

**Example Message:**
```json
{
  "id": "queue-item-456",
  "labId": "673a1b2c3d4e5f6789abcdef",
  "groupId": "group-beta-002",
  "status": "CLAIMED",
  "previousStatus": "WAITING",
  "priority": "NORMAL",
  "position": 3,
  "requestedBy": "user-student-789",
  "requestedByName": "Emma Wilson",
  "claimedBy": "user-ta-456",
  "claimedByName": "Alex Martinez (TA)",
  "timestamp": "2025-11-04T23:30:00.000Z",
  "description": "Need help with Git merge conflicts"
}
```

---

## Frontend Usage

### Setup (React Component Example)

```typescript
import { useEffect, useState } from 'react';
import { websocketService } from '../services/websocketServiceEnhanced';
import { CheckpointUpdate, GroupStatusUpdate, HelpQueueUpdate } from '../types/websocket';

function LabView({ labId }: { labId: string }) {
  const [checkpoints, setCheckpoints] = useState<CheckpointUpdate[]>([]);
  const [groupStatuses, setGroupStatuses] = useState<GroupStatusUpdate[]>([]);
  const [helpQueue, setHelpQueue] = useState<HelpQueueUpdate[]>([]);

  useEffect(() => {
    // Initialize WebSocket connection
    websocketService.init();

    // Subscribe to all lab updates
    websocketService.subscribeToLab(labId);

    // Register listeners
    const handleCheckpointUpdate = (update: CheckpointUpdate) => {
      console.log('Checkpoint updated:', update);
      setCheckpoints((prev) => [...prev, update]);
      // Update UI accordingly
    };

    const handleGroupStatusUpdate = (update: GroupStatusUpdate) => {
      console.log('Group status changed:', update);
      setGroupStatuses((prev) => [...prev, update]);
    };

    const handleHelpQueueUpdate = (update: HelpQueueUpdate) => {
      console.log('Help queue updated:', update);
      setHelpQueue((prev) => {
        const index = prev.findIndex(item => item.id === update.id);
        if (index >= 0) {
          const newQueue = [...prev];
          newQueue[index] = update;
          return newQueue;
        }
        return [...prev, update];
      });
    };

    websocketService.onCheckpointUpdate(handleCheckpointUpdate);
    websocketService.onGroupStatusUpdate(handleGroupStatusUpdate);
    websocketService.onHelpQueueUpdate(handleHelpQueueUpdate);

    // Cleanup on unmount
    return () => {
      websocketService.offCheckpointUpdate(handleCheckpointUpdate);
      websocketService.offGroupStatusUpdate(handleGroupStatusUpdate);
      websocketService.offHelpQueueUpdate(handleHelpQueueUpdate);
      websocketService.unsubscribeFromLab(labId);
    };
  }, [labId]);

  return (
    <div>
      <h2>Lab View</h2>
      {/* Render checkpoints, groups, help queue */}
    </div>
  );
}
```

### Connection Status Monitoring

```typescript
useEffect(() => {
  const handleStatusChange = (status: 'CONNECTED' | 'RECONNECTING' | 'DISCONNECTED') => {
    console.log('WebSocket status:', status);
    setConnectionStatus(status);
  };

  websocketService.onStatusChange(handleStatusChange);

  return () => {
    websocketService.offStatusChange(handleStatusChange);
  };
}, []);
```

---

## Backend Broadcasting

### From LabController (Checkpoint Signoff)

```java
@Autowired
private LabWebSocketController wsController;

// When signing off a checkpoint
CheckpointUpdate update = new CheckpointUpdate(labId, groupId, checkpointNumber, "PASS");
update.setSignedOffBy(userId);
update.setSignedOffByName(userName);
update.setPointsAwarded(1);
update.setNotes(notes);

wsController.broadcastCheckpointUpdate(labId, update);
```

### From GroupService (Status Change)

```java
GroupStatusUpdate update = new GroupStatusUpdate(labId, groupId, GroupStatus.SIGNED_OFF);
update.setPreviousStatus(group.getStatus());
update.setPerformedBy(userId);
update.setPerformedByName(userName);
update.setTotalScore(group.getTotalScore());
update.setFinalGrade(group.getFinalGrade());

wsController.broadcastGroupStatusUpdate(labId, update);
```

### From HelpQueueService (Queue Update)

```java
HelpQueueUpdate update = new HelpQueueUpdate(queueItem.getId(), labId, groupId, HelpQueueStatus.CLAIMED);
update.setPreviousStatus(HelpQueueStatus.WAITING);
update.setClaimedBy(taUserId);
update.setClaimedByName(taName);
update.setPosition(queueItem.getPosition());

wsController.broadcastHelpQueueUpdate(labId, update);
```

---

## Testing

### Backend Test Endpoint

```bash
# Test WebSocket broadcast
curl http://localhost:8080/ws-test-broadcast

# Response:
✅ Test WebSocket broadcast sent to /topic/labs/test-lab-123/checkpoints
```

### Frontend Test (Browser Console)

```javascript
// In browser console
import { websocketService } from './services/websocketServiceEnhanced';

// Initialize
websocketService.init();

// Subscribe to test lab
websocketService.subscribeToLab('test-lab-123');

// Add listener
websocketService.onCheckpointUpdate((update) => {
  console.log('Received checkpoint update:', update);
});

// Trigger test broadcast from backend
fetch('http://localhost:8080/ws-test-broadcast');
```

---

## Connection Flow

```
Frontend                                    Backend
   |                                          |
   |  1. new SockJS('/ws')                   |
   |------------------------------------->    |
   |                                          |
   |  2. STOMP CONNECT                        |
   |------------------------------------->    |
   |                                          |
   |  3. CONNECTED (onConnect callback)       |
   |<-------------------------------------    |
   |                                          |
   |  4. SUBSCRIBE /topic/labs/{labId}/*      |
   |------------------------------------->    |
   |                                          |
   |  5. Subscribed (confirmation)            |
   |<-------------------------------------    |
   |                                          |
   |  [Teacher signs off checkpoint]          |
   |                                          |
   |  6. MESSAGE /topic/labs/{labId}/checkpoints
   |<-------------------------------------    |
   |     { labId, groupId, checkpoint... }    |
   |                                          |
   |  7. Parse JSON, notify listeners         |
   |                                          |
```

---

## Troubleshooting

### Connection Issues

**Problem:** WebSocket won't connect

**Solutions:**
1. Check CORS configuration in `WebSocketConfig.java`
2. Verify SockJS endpoint is `/ws`
3. Check frontend URL matches backend: `http://localhost:8080/ws`
4. Look for errors in browser console

### Message Not Received

**Problem:** Frontend subscribed but not receiving messages

**Solutions:**
1. Verify topic path matches: `/topic/labs/{labId}/checkpoints`
2. Check backend is broadcasting to correct topic
3. Ensure `labId` matches between subscription and broadcast
4. Add debug logging to see STOMP frames

### Type Mismatches

**Problem:** TypeScript errors or runtime parsing failures

**Solutions:**
1. Ensure backend DTOs match frontend interfaces
2. Check JSON serialization settings in Spring Boot
3. Verify `Instant` is serialized to ISO 8601 string format
4. Use `JSON.parse()` error handling

---

## Migration from Old WebSocket Implementation

### Old Code (websocketService.ts)

```typescript
// OLD - single topic for all updates
websocketService.subscribeToGroup(groupId);
websocketService.addListener((update: CheckpointUpdate) => {
  // Handle update
});
```

### New Code (websocketServiceEnhanced.ts)

```typescript
// NEW - lab-specific and type-safe
websocketService.subscribeToLab(labId);
websocketService.onCheckpointUpdate((update: CheckpointUpdate) => {
  // Handle checkpoint update
});
websocketService.onGroupStatusUpdate((update: GroupStatusUpdate) => {
  // Handle group status update
});
websocketService.onHelpQueueUpdate((update: HelpQueueUpdate) => {
  // Handle help queue update
});
```

### Backward Compatibility

Legacy methods are still available but deprecated:

```java
@Deprecated
public void broadcastCheckpointUpdate(String groupId, int checkpointNumber, String status)

@Deprecated
public void broadcastGroupPassed(String groupId)
```

These will log warnings but continue to work to prevent breaking existing code.

---

## Best Practices

1. **Always unsubscribe on unmount** - Prevent memory leaks
2. **Filter messages by labId** - Even with lab-specific topics, validate the labId matches
3. **Handle connection loss gracefully** - Show reconnection status to users
4. **Debounce rapid updates** - If receiving many checkpoint updates, batch UI updates
5. **Log errors** - Help debugging in production

---

## Files Modified

### Backend
- `CheckpointUpdate.java` - Enhanced with new fields
- `LabWebSocketController.java` - New broadcast methods
- `websocket/GroupStatusUpdate.java` - New DTO (created)
- `websocket/HelpQueueUpdate.java` - New DTO (created)

### Frontend
- `types/websocket.ts` - Type definitions (created)
- `services/websocketServiceEnhanced.ts` - Enhanced service (created)
- `services/websocketService.ts` - Legacy service (unchanged for backward compatibility)

---

## Next Steps for Frontend Team

1. Import the new `websocketServiceEnhanced.ts` service
2. Update components to use new subscription methods
3. Add type imports from `types/websocket.ts`
4. Test connection with backend using `/ws-test-broadcast` endpoint
5. Implement UI updates for real-time checkpoint/group/queue changes
6. Add connection status indicator to user interface
