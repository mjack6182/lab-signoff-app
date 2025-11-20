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
        int totalCheckpoints = lab.getCheckpoints().size();

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

    // -----------------------------------------------------
    // BELOW ARE YOUR CUSTOM METHODS (unchanged)
    // -----------------------------------------------------

    public List<Group> randomizeGroups(String labId) {
        Lab lab = labService.getAll().stream()
                .filter(l -> l.getId().equals(labId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Lab not found with id: " + labId));

        String classId = lab.getClassId();
        int minGroupSize = lab.getMinGroupSize() != null ? lab.getMinGroupSize() : 2;
        int maxGroupSize = lab.getMaxGroupSize() != null ? lab.getMaxGroupSize() : 3;

        List<Enrollment> studentEnrollments = enrollmentService.getActiveStudents(classId);
        if (studentEnrollments.isEmpty()) {
            throw new RuntimeException("No active students enrolled in the class");
        }

        List<User> students = studentEnrollments.stream()
                .map(Enrollment::getUserId)
                .map(userId -> userService.getAllUsers().stream()
                        .filter(u -> u.getId().equals(userId))
                        .findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (students.isEmpty()) {
            throw new RuntimeException("No valid student users found");
        }

        List<Group> existingGroups = repo.findByLabId(labId);
        int generationNumber = existingGroups.stream()
                .mapToInt(g -> g.getGenerationNumber() != null ? g.getGenerationNumber() : 0)
                .max()
                .orElse(0) + 1;

        repo.deleteAll(existingGroups);

        Collections.shuffle(students);

        int totalStudents = students.size();
        int numGroups = calculateOptimalGroupCount(totalStudents, minGroupSize, maxGroupSize);

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

            int studentsPerGroup = totalStudents / numGroups;
            int remainder = totalStudents % numGroups;
            int studentsForThisGroup = studentsPerGroup + (groupNum <= remainder ? 1 : 0);

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

        return repo.saveAll(newGroups);
    }

    public List<Group> bulkUpdateGroups(String labId, List<Group> groups) {
        Lab lab = labService.getAll().stream()
                .filter(l -> l.getId().equals(labId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Lab not found with id: " + labId));

        String classId = lab.getClassId();

        List<Enrollment> studentEnrollments = enrollmentService.getActiveStudents(classId);
        List<String> validStudentIds = studentEnrollments.stream()
                .map(Enrollment::getUserId)
                .collect(Collectors.toList());

        for (Group group : groups) {
            if (group.getMembers() != null) {
                for (GroupMember member : group.getMembers()) {
                    if (!validStudentIds.contains(member.getUserId())) {
                        throw new RuntimeException("Invalid student id " + member.getUserId());
                    }
                }
            }
        }

        List<Group> existingGroups = repo.findByLabId(labId);
        repo.deleteAll(existingGroups);

        Instant now = Instant.now();
        for (Group group : groups) {
            group.setLabId(labId);
            if (group.getCreatedAt() == null) {
                group.setCreatedAt(now);
            }
            group.setLastUpdatedAt(now);
            if (group.getStatus() == null) {
                group.setStatus(GroupStatus.FORMING);
            }
        }

        return repo.saveAll(groups);
    }

    public void deleteGroup(String groupId) {
        repo.deleteById(groupId);
    }

    private int calculateOptimalGroupCount(int totalStudents, int minGroupSize, int maxGroupSize) {
        int numGroups = (int) Math.ceil((double) totalStudents / maxGroupSize);

        while (numGroups > 1) {
            int studentsPerGroup = totalStudents / numGroups;
            if (studentsPerGroup >= minGroupSize) break;
            numGroups--;
        }

        return Math.max(1, numGroups);
    }
}
