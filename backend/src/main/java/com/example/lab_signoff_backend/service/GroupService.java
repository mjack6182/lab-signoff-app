package com.example.lab_signoff_backend.service;
import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.model.User;
import com.example.lab_signoff_backend.model.Enrollment;
import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import com.example.lab_signoff_backend.repository.GroupRepository;
import com.example.lab_signoff_backend.repository.LabRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository repo;
    private final LabRepository labRepo;
    private final LabService labService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    /**
     * Constructor for GroupService.
     *
     * @param repo The GroupRepository for database operations
     * @param labRepo The LabRepository for direct database operations
     * @param labService The LabService for lab operations
     * @param enrollmentService The EnrollmentService for enrollment operations
     * @param userService The UserService for user operations
     */
    public GroupService(GroupRepository repo, LabRepository labRepo, LabService labService,
                       EnrollmentService enrollmentService, UserService userService) {
        this.repo = repo;
        this.labRepo = labRepo;
        this.labService = labService;
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

    public List<Group> getGroupsByLabId(String labId) {
        List<Group> groups = repo.findByLabId(labId);
        for (Group g : groups) {
            autoInitCheckpoints(g);
        }
        return groups;
    }

    public List<Group> getAll() {
        List<Group> groups = repo.findAll();
        for (Group g : groups) {
            autoInitCheckpoints(g);
        }
        return groups;
    }


    public Group upsert(Group group) {
        autoInitCheckpoints(group);
        return repo.save(group);
    }

    /**
     * Fetch a group by its Mongo document id or display groupId.
     * Auto-initialises checkpoint progress to avoid nulls when returned to callers.
     */
    public Optional<Group> getById(String idOrGroupId) {
        Optional<Group> groupOpt = repo.findById(idOrGroupId);

        if (groupOpt.isEmpty()) {
            groupOpt = repo.findByGroupId(idOrGroupId);
        }

        groupOpt.ifPresent(this::autoInitCheckpoints);
        return groupOpt;
    }

    public CheckpointProgress updateCheckpointProgress(
            String groupIdOrId,
            Integer checkpointNumber,
            String statusString,
            String signedOffBy,
            String signedOffByName,
            String notes,
            Integer pointsAwarded) {

        Optional<Group> maybeGroup = repo.findByGroupId(groupIdOrId);
        if (maybeGroup.isEmpty()) {
            maybeGroup = repo.findById(groupIdOrId);
        }

        if (maybeGroup.isEmpty()) {
            throw new RuntimeException("Group not found: " + groupIdOrId);
        }

        Group group = maybeGroup.get();

        autoInitCheckpoints(group);

        List<CheckpointProgress> progressList = group.getCheckpointProgress();

        CheckpointProgress target = progressList.stream()
                .filter(cp -> Objects.equals(cp.getCheckpointNumber(), checkpointNumber))
                .findFirst()
                .orElse(null);

        if (target == null) {
            target = new CheckpointProgress();
            target.setCheckpointNumber(checkpointNumber);
            progressList.add(target);
        }

        SignoffAction action = SignoffAction.valueOf(statusString);
        target.setStatus(action);
        target.setSignedOffBy(signedOffBy);
        target.setSignedOffByName(signedOffByName);
        target.setTimestamp(Instant.now());
        target.setNotes(notes);
        target.setPointsAwarded(pointsAwarded);

        group.updateTimestamp();
        Group saved = repo.save(group);

        return saved.getCheckpointProgress().stream()
                .filter(cp -> Objects.equals(cp.getCheckpointNumber(), checkpointNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Failed to persist checkpoint progress"));
    }

    private void autoInitCheckpoints(Group group) {
        if (group.getCheckpointProgress() != null && !group.getCheckpointProgress().isEmpty()) {
            return;
        }

        Optional<Lab> labOpt = labRepo.findById(group.getLabId());
        if (labOpt.isEmpty()) {
            throw new RuntimeException("Lab not found for group " + group.getId());
        }

        Lab lab = labOpt.get();
        int totalCheckpoints = lab.getPoints();

        List<CheckpointProgress> list = new ArrayList<>();

        for (int i = 1; i <= totalCheckpoints; i++) {
            CheckpointProgress cp = new CheckpointProgress();
            cp.setCheckpointNumber(i);
            cp.setStatus(null);
            cp.setSignedOffBy(null);
            cp.setSignedOffByName(null);
            cp.setTimestamp(null);
            cp.setNotes("");
            cp.setPointsAwarded(null);
            list.add(cp);
        }

        group.setCheckpointProgress(list);
        repo.save(group);
    }

    /**
     * Randomize groups for a lab based on enrolled students
     *
     * This method will:
     * 1. Delete all existing groups for the lab
     * 2. Get all active enrolled students from the class
     * 3. Shuffle students randomly
     * 4. Distribute them into groups respecting min/max size constraints
     * 5. Handle remainder students by distributing them across groups
     * 6. Increment generation number
     *
     * @param labId The lab identifier
     * @return List of newly created groups
     * @throws RuntimeException if lab not found or no students enrolled
     */
    public List<Group> randomizeGroups(String labId) {
        // Get the lab to access classId and group size settings
        Lab lab = labService.getAll().stream()
                .filter(l -> l.getId().equals(labId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Lab not found with id: " + labId));

        String classId = lab.getClassId();
        int minGroupSize = lab.getMinGroupSize() != null ? lab.getMinGroupSize() : 2;
        int maxGroupSize = lab.getMaxGroupSize() != null ? lab.getMaxGroupSize() : 3;

        // Get all active students enrolled in the class
        List<Enrollment> studentEnrollments = enrollmentService.getActiveStudents(classId);

        if (studentEnrollments.isEmpty()) {
            throw new RuntimeException("No active students enrolled in the class");
        }

        // Get user details for all enrolled students
        List<User> students = studentEnrollments.stream()
                .map(enrollment -> enrollment.getUserId())
                .map(userId -> userService.getAllUsers().stream()
                        .filter(u -> u.getId().equals(userId))
                        .findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (students.isEmpty()) {
            throw new RuntimeException("No valid student users found for enrolled students");
        }

        // Calculate generation number (max existing + 1, or 1 if no groups exist)
        List<Group> existingGroups = repo.findByLabId(labId);
        int generationNumber = existingGroups.stream()
                .mapToInt(g -> g.getGenerationNumber() != null ? g.getGenerationNumber() : 0)
                .max()
                .orElse(0) + 1;

        // Delete all existing groups for this lab
        repo.deleteAll(existingGroups);

        // Shuffle students randomly
        Collections.shuffle(students);

        // Calculate optimal group distribution
        int totalStudents = students.size();
        int numGroups = calculateOptimalGroupCount(totalStudents, minGroupSize, maxGroupSize);

        // Create groups and distribute students
        List<Group> newGroups = new ArrayList<>();
        int studentIndex = 0;

        for (int groupNum = 1; groupNum <= numGroups; groupNum++) {
            Group group = new Group();
            group.setLabId(labId);
            group.setGroupId("Group-" + groupNum);
            group.setGroupNumber(groupNum);
            group.setStatus(GroupStatus.FORMING);
            group.setGenerationNumber(generationNumber);
            group.setCreatedAt(Instant.now());

            List<GroupMember> members = new ArrayList<>();

            // Calculate how many students this group should get
            int studentsPerGroup = totalStudents / numGroups;
            int remainder = totalStudents % numGroups;

            // Distribute remainder students across first groups
            int studentsForThisGroup = studentsPerGroup + (groupNum <= remainder ? 1 : 0);

            // Add students to this group
            for (int i = 0; i < studentsForThisGroup && studentIndex < totalStudents; i++) {
                User student = students.get(studentIndex++);
                GroupMember member = new GroupMember();
                member.setUserId(student.getId());
                member.setName(student.getName());
                member.setEmail(student.getEmail());
                member.setJoinedAt(Instant.now());
                member.setPresent(true);
                members.add(member);
            }

            group.setMembers(members);
            newGroups.add(group);
        }

        // Save all groups
        return repo.saveAll(newGroups);
    }

    /**
     * Bulk update groups for a lab
     *
     * This method will:
     * 1. Delete all existing groups for the lab
     * 2. Validate that all student IDs belong to enrolled students in the class
     * 3. Create/update groups based on the provided list
     *
     * @param labId The lab identifier
     * @param groups List of groups to save
     * @return List of saved groups
     * @throws RuntimeException if lab not found or validation fails
     */
    public List<Group> bulkUpdateGroups(String labId, List<Group> groups) {
        // Get the lab to access classId for validation
        Lab lab = labService.getAll().stream()
                .filter(l -> l.getId().equals(labId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Lab not found with id: " + labId));

        String classId = lab.getClassId();

        // Get all active students enrolled in the class for validation
        List<Enrollment> studentEnrollments = enrollmentService.getActiveStudents(classId);
        List<String> validStudentIds = studentEnrollments.stream()
                .map(Enrollment::getUserId)
                .collect(Collectors.toList());

        // Validate all student IDs in the groups belong to the class
        for (Group group : groups) {
            if (group.getMembers() != null) {
                for (GroupMember member : group.getMembers()) {
                    if (!validStudentIds.contains(member.getUserId())) {
                        throw new RuntimeException(
                            "Student with ID " + member.getUserId() +
                            " is not enrolled in the class associated with this lab"
                        );
                    }
                }
            }
        }

        // Delete all existing groups for this lab
        List<Group> existingGroups = repo.findByLabId(labId);
        repo.deleteAll(existingGroups);

        // Ensure all groups have the correct labId and set timestamps
        Instant now = Instant.now();
        for (Group group : groups) {
            group.setLabId(labId);
            if (group.getCreatedAt() == null) {
                group.setCreatedAt(now);
            }
            group.setLastUpdatedAt(now);

            // Set default status if not provided
            if (group.getStatus() == null) {
                group.setStatus(GroupStatus.FORMING);
            }
        }

        // Save all groups
        return repo.saveAll(groups);
    }

    /**
     * Delete a group by its ID
     *
     * @param groupId The MongoDB document ID of the group to delete
     */
    public void deleteGroup(String groupId) {
        repo.deleteById(groupId);
    }

    /**
     * Calculate the optimal number of groups given total students and size constraints
     *
     * @param totalStudents Total number of students to distribute
     * @param minGroupSize Minimum group size
     * @param maxGroupSize Maximum group size
     * @return Optimal number of groups
     */
    private int calculateOptimalGroupCount(int totalStudents, int minGroupSize, int maxGroupSize) {
        // Try to create groups as close to maxGroupSize as possible
        int numGroups = (int) Math.ceil((double) totalStudents / maxGroupSize);

        // Ensure no group will be smaller than minGroupSize after distribution
        while (numGroups > 1) {
            int studentsPerGroup = totalStudents / numGroups;
            int remainder = totalStudents % numGroups;

            // Check if the smallest group will meet minimum size
            int smallestGroupSize = studentsPerGroup;
            if (remainder > 0) {
                // Some groups will have studentsPerGroup + 1
                smallestGroupSize = studentsPerGroup;
            }

            if (smallestGroupSize >= minGroupSize) {
                break;
            }

            numGroups--;
        }

        return Math.max(1, numGroups);
    }
}
