# Database Schema Implementation Status

## Overview
This document tracks the implementation of the new MongoDB database schema as defined in [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md).

**Last Updated**: 2025-01-04
**Status**: In Progress (Phase 1 Complete)

---

## ✅ Phase 1: Completed Components

### 1. Enums Created
All enum types have been implemented with proper Java enum syntax:

- ✅ **LabStatus** - [backend/src/main/java/com/example/lab_signoff_backend/model/enums/LabStatus.java](backend/src/main/java/com/example/lab_signoff_backend/model/enums/LabStatus.java)
  - `DRAFT`, `ACTIVE`, `CLOSED`, `ARCHIVED`

- ✅ **GroupStatus** - [backend/src/main/java/com/example/lab_signoff_backend/model/enums/GroupStatus.java](backend/src/main/java/com/example/lab_signoff_backend/model/enums/GroupStatus.java)
  - `FORMING`, `IN_PROGRESS`, `COMPLETED`, `SIGNED_OFF`

- ✅ **SignoffAction** - [backend/src/main/java/com/example/lab_signoff_backend/model/enums/SignoffAction.java](backend/src/main/java/com/example/lab_signoff_backend/model/enums/SignoffAction.java)
  - `PASS`, `RETURN`, `COMPLETE`

- ✅ **EnrollmentStatus** - [backend/src/main/java/com/example/lab_signoff_backend/model/enums/EnrollmentStatus.java](backend/src/main/java/com/example/lab_signoff_backend/model/enums/EnrollmentStatus.java)
  - `ACTIVE`, `DROPPED`, `COMPLETED`

- ✅ **EnrollmentRole** - [backend/src/main/java/com/example/lab_signoff_backend/model/enums/EnrollmentRole.java](backend/src/main/java/com/example/lab_signoff_backend/model/enums/EnrollmentRole.java)
  - `STUDENT`, `TA`, `TEACHER`

- ✅ **HelpQueueStatus** - [backend/src/main/java/com/example/lab_signoff_backend/model/enums/HelpQueueStatus.java](backend/src/main/java/com/example/lab_signoff_backend/model/enums/HelpQueueStatus.java)
  - `WAITING`, `CLAIMED`, `RESOLVED`, `CANCELLED`

- ✅ **HelpQueuePriority** - [backend/src/main/java/com/example/lab_signoff_backend/model/enums/HelpQueuePriority.java](backend/src/main/java/com/example/lab_signoff_backend/model/enums/HelpQueuePriority.java)
  - `NORMAL`, `URGENT`

### 2. Embedded Documents Created
All embedded subdocuments for nested structures:

- ✅ **GroupMember** - [backend/src/main/java/com/example/lab_signoff_backend/model/embedded/GroupMember.java](backend/src/main/java/com/example/lab_signoff_backend/model/embedded/GroupMember.java)
  - Fields: `userId`, `name`, `email`, `joinedAt`, `present`
  - Validation: `@NotBlank`, `@Email`, `@NotNull`

- ✅ **CheckpointDefinition** - [backend/src/main/java/com/example/lab_signoff_backend/model/embedded/CheckpointDefinition.java](backend/src/main/java/com/example/lab_signoff_backend/model/embedded/CheckpointDefinition.java)
  - Fields: `number`, `name`, `description`, `points`, `required`
  - Embedded in Lab documents

- ✅ **CheckpointProgress** - [backend/src/main/java/com/example/lab_signoff_backend/model/embedded/CheckpointProgress.java](backend/src/main/java/com/example/lab_signoff_backend/model/embedded/CheckpointProgress.java)
  - Fields: `checkpointNumber`, `status`, `signedOffBy`, `signedOffByName`, `timestamp`, `notes`, `pointsAwarded`
  - Embedded in Group documents to track per-checkpoint progress

- ✅ **CanvasMetadata** - [backend/src/main/java/com/example/lab_signoff_backend/model/embedded/CanvasMetadata.java](backend/src/main/java/com/example/lab_signoff_backend/model/embedded/CanvasMetadata.java)
  - Fields: `lineItemId`, `courseId`, `contextId`, `deploymentId`, `resourceLinkId`
  - Embedded in Class documents

### 3. New Entity Models Created

- ✅ **Class** - [backend/src/main/java/com/example/lab_signoff_backend/model/Class.java](backend/src/main/java/com/example/lab_signoff_backend/model/Class.java)
  - Collection: `classes`
  - Indexes: `courseCode`, `instructorId`
  - Features:
    - Roster management (student IDs from Canvas CSV)
    - TA assignment tracking
    - Canvas metadata integration
    - Helper methods: `addStudentToRoster()`, `addTA()`, `isStaff()`, etc.

- ✅ **Enrollment** - [backend/src/main/java/com/example/lab_signoff_backend/model/Enrollment.java](backend/src/main/java/com/example/lab_signoff_backend/model/Enrollment.java)
  - Collection: `enrollments`
  - Compound Indexes:
    - `(classId, role)` - Find TAs/students
    - `(userId, classId)` - Unique constraint
  - Features:
    - TA upgrade tracking (`upgradeRequestedBy`)
    - Status transitions: `drop()`, `complete()`
    - Role helpers: `isStudent()`, `isTA()`, `isStaff()`

- ✅ **HelpQueueItem** - [backend/src/main/java/com/example/lab_signoff_backend/model/HelpQueueItem.java](backend/src/main/java/com/example/lab_signoff_backend/model/HelpQueueItem.java)
  - Collection: `help_queue_items`
  - Compound Indexes:
    - `(labId, status)` - Find waiting requests
    - `(labId, position)` - Queue ordering
  - Features:
    - Per-lab help queue
    - State transitions: `claim()`, `resolve()`, `cancel()`
    - Priority support (normal/urgent)
    - Position tracking in queue

### 4. Updated Entity Models

- ✅ **Lab** - [backend/src/main/java/com/example/lab_signoff_backend/model/Lab.java](backend/src/main/java/com/example/lab_signoff_backend/model/Lab.java)
  - **COMPLETELY REWRITTEN** with new schema
  - New fields:
    - `title`, `description`, `points`
    - `joinCode` (unique, auto-generated 6-char)
    - `status` (LabStatus enum)
    - `startTime`, `endTime`
    - `maxGroupSize`, `minGroupSize`
    - `autoRandomize` (for group formation)
    - `checkpoints[]` (embedded CheckpointDefinition)
    - `createdAt`, `updatedAt`, `createdBy`
  - Features:
    - Auto-generates join codes (`generateJoinCode()`)
    - Auto-initializes checkpoints based on points (1 point = 1 checkpoint)
    - Status management: `activate()`, `close()`, `archive()`
    - Joinability check: `isJoinable()`
  - Legacy compatibility: `@Deprecated` methods for `courseId`/`lineItemId`

### 5. Dependencies Updated

- ✅ **build.gradle** - [backend/build.gradle:34](backend/build.gradle#L34)
  - Added `spring-boot-starter-validation` for Jakarta validation annotations
  - Build successful - all dependencies downloaded

---

## 🚧 Phase 2: Pending Components

### Entity Models to Update

1. **Group** - Needs significant updates:
   - ❌ Change `members` from `List<String>` to `List<GroupMember>` (embedded)
   - ❌ Change `status` from `String` to `GroupStatus` enum
   - ❌ Add `checkpointProgress` (List<CheckpointProgress>)
   - ❌ Add `groupNumber`, `currentCheckpoint`, `totalScore`, `finalGrade`
   - ❌ Add timestamps: `createdAt`, `lastUpdatedAt`, `completedAt`
   - ❌ Add `generationNumber` for re-randomization support
   - ❌ Add helper methods for checkpoint management

2. **SignoffEvent** - Needs updates:
   - ❌ Change `action` from `String` to `SignoffAction` enum
   - ❌ Make `checkpointNumber` required (currently optional)
   - ❌ Add `performerRole` field
   - ❌ Add validation annotations

3. **User** - Minor enhancement:
   - ❌ Add `roleHistory[]` for audit trail of role upgrades
   - Current model is mostly compatible

### Repository Interfaces to Create

All repositories need to be created with custom query methods:

1. **ClassRepository**
   ```java
   List<Class> findByInstructorId(String instructorId);
   List<Class> findByTermAndArchived(String term, Boolean archived);
   Optional<Class> findByCourseCodeAndTerm(String courseCode, String term);
   ```

2. **EnrollmentRepository**
   ```java
   List<Enrollment> findByUserId(String userId);
   List<Enrollment> findByClassId(String classId);
   List<Enrollment> findByClassIdAndRole(String classId, EnrollmentRole role);
   List<Enrollment> findByClassIdAndStatus(String classId, EnrollmentStatus status);
   Optional<Enrollment> findByUserIdAndClassId(String userId, String classId);
   boolean existsByUserIdAndClassId(String userId, String classId);
   ```

3. **HelpQueueItemRepository**
   ```java
   List<HelpQueueItem> findByLabIdAndStatus(String labId, HelpQueueStatus status);
   List<HelpQueueItem> findByLabIdAndStatusOrderByPositionAsc(String labId, HelpQueueStatus status);
   List<HelpQueueItem> findByClaimedBy(String userId);
   List<HelpQueueItem> findByRaisedBy(String userId);
   Optional<HelpQueueItem> findByLabIdAndGroupIdAndStatus(String labId, String groupId, HelpQueueStatus status);
   ```

4. **Update existing repositories**:
   - LabRepository - add queries for `findByClassId`, `findByJoinCode`, `findByClassIdAndStatus`
   - GroupRepository - add queries for checkpoint progress filtering
   - SignoffEventRepository - already has good queries

### Service Layer to Create/Update

1. **ClassService** - NEW
   - CRUD operations
   - Canvas CSV import logic
   - Roster management
   - TA assignment

2. **EnrollmentService** - NEW
   - Enroll students
   - Upgrade to TA
   - Drop/complete enrollment

3. **HelpQueueService** - NEW
   - Raise hand
   - Claim request
   - Resolve/cancel request
   - Queue position management

4. **Update existing services**:
   - LabService - update for new Lab model
   - GroupService - update for checkpoint progress
   - SignoffEventService - update for enum types

### Controller Endpoints to Create/Update

1. **ClassController** - NEW
   - `POST /api/classes` - Create class
   - `POST /api/classes/{id}/roster/import` - Import Canvas CSV
   - `GET /api/classes/{id}` - Get class details
   - `GET /api/classes` - List classes for instructor
   - `POST /api/classes/{id}/tas/{userId}` - Assign TA

2. **EnrollmentController** - NEW
   - `POST /api/enrollments` - Enroll student
   - `PUT /api/enrollments/{id}/upgrade` - Upgrade to TA
   - `GET /api/enrollments/user/{userId}` - Get user's enrollments
   - `GET /api/enrollments/class/{classId}` - Get class roster

3. **HelpQueueController** - NEW
   - `POST /api/labs/{labId}/queue` - Raise hand
   - `PUT /api/queue/{id}/claim` - Claim request
   - `PUT /api/queue/{id}/resolve` - Resolve request
   - `GET /api/labs/{labId}/queue` - Get queue for lab

4. **Update LabController**:
   - Update to use new Lab model
   - Add join code validation
   - Add lab activation endpoints

---

## 📋 Migration Plan

### Data Migration Steps

1. **Backup existing data**
   ```bash
   mongodump --uri="<MONGODB_URI>" --out=./backup-$(date +%Y%m%d)
   ```

2. **Update Lab documents**:
   - Add `title`, `points`, `joinCode`, `status`, `checkpoints[]`
   - Generate join codes for existing labs
   - Initialize checkpoints based on lab requirements
   - Set default `maxGroupSize=3`, `minGroupSize=2`

3. **Update Group documents**:
   - Convert `members` from strings to GroupMember objects
   - Convert `status` string to enum
   - Add `checkpointProgress[]` array
   - Add timestamps

4. **Update SignoffEvent documents**:
   - Convert `action` string to enum
   - Ensure `checkpointNumber` is populated

5. **Create Class documents** (if importing from Canvas):
   - Parse Canvas CSV exports
   - Create Class documents
   - Link existing labs to classes

---

## 🎯 Next Steps (Priority Order)

### Immediate (Complete Phase 2):
1. Update `Group.java` model with new schema
2. Update `SignoffEvent.java` model with enums
3. Create all repository interfaces
4. Update existing services for new models

### Short-term:
5. Create new service classes (ClassService, EnrollmentService, HelpQueueService)
6. Create new controllers
7. Write data migration scripts
8. Create comprehensive unit tests

### Medium-term:
9. Implement Canvas CSV import functionality
10. Implement auto-randomization for group formation
11. Implement grade export to CSV
12. Add MongoDB indexes via configuration

---

## 📊 Implementation Statistics

| Component | Total | Completed | Remaining |
|-----------|-------|-----------|-----------|
| Enums | 7 | 7 ✅ | 0 |
| Embedded Documents | 4 | 4 ✅ | 0 |
| Entity Models | 7 | 4 ✅ | 3 |
| Repositories | 7 | 4 (existing) | 3 (new) |
| Services | 7 | 4 (existing) | 3 (new) |
| Controllers | 7 | 4 (existing) | 3 (new) |

**Overall Progress**: ~50% Complete

---

## 🔍 Testing Checklist

### Unit Tests Needed:
- [ ] Lab join code generation (uniqueness)
- [ ] Checkpoint initialization (1 point = 1 checkpoint)
- [ ] Group member management
- [ ] Enrollment role transitions
- [ ] Help queue position management
- [ ] Canvas CSV parsing

### Integration Tests Needed:
- [ ] Complete student join flow
- [ ] Teacher creates lab → students join → groups formed
- [ ] Checkpoint sign-off flow
- [ ] Help queue claim and resolve flow
- [ ] Grade calculation and export

---

## 📚 Related Documentation

- [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) - Complete schema design with diagrams
- [Backend README](backend/README.md) - Backend setup and configuration
- [API Documentation](http://localhost:8080/swagger-ui.html) - Swagger UI (when running)

---

## 💡 Notes

### IDE Validation Errors
The IDE is currently showing errors for `jakarta.validation` imports. This is expected and will resolve automatically once the IDE refreshes its dependency cache. The Gradle build is successful and all dependencies are available.

To force IDE refresh:
- **VS Code**: Reload window (Cmd+Shift+P → "Reload Window")
- **IntelliJ**: File → Invalidate Caches → Restart

### Backward Compatibility
The updated `Lab` model includes `@Deprecated` methods (`getCourseId()`, `getLineItemId()`) for backward compatibility with existing code. These should be migrated to use `getClassId()` instead.

### Performance Considerations
Compound indexes have been defined in the entity annotations (`@CompoundIndexes`). Spring Data MongoDB will automatically create these indexes when the application starts, but for production, you should create them manually before deployment for better control.

---

## 🤝 Team Coordination

### Who's Working on What:
- **Canvas CSV Import**: [Teammate working on this]
- **Database Schema**: ✅ Models created (this implementation)
- **Frontend Student Flow**: ✅ Routes configured, mock data created
- **Backend API**: 🚧 Needs completion (Phase 2)

### Questions for Team Discussion:
1. How should we handle Canvas CSV format variations?
2. Should join codes expire after a certain time?
3. What happens if a student tries to join multiple groups in the same lab?
4. Should TAs be auto-assigned to all labs in their class, or per-lab?
5. How should we handle timezone differences for lab deadlines?

---

**Generated by**: Claude Code
**Date**: 2025-01-04
