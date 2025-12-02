package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Class;
import com.example.lab_signoff_backend.model.Enrollment;
import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.embedded.CheckpointDefinition;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.User;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import com.example.lab_signoff_backend.service.ClassService;
import com.example.lab_signoff_backend.service.EnrollmentService;
import com.example.lab_signoff_backend.service.GroupService;
import com.example.lab_signoff_backend.service.LabService;
import com.example.lab_signoff_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller exposing student-facing lab endpoints (join + detail lookup).
 */
@RestController
@RequestMapping("/api/labs")
@CrossOrigin(
        origins = {
                "http://localhost:5173",
                "http://localhost:5002",
                "https://lab-signoff-app.web.app",
                "https://lab-signoff-app.firebaseapp.com"
        },
        allowCredentials = "true"
)
public class LabJoinController {

    private final LabService labService;
    private final ClassService classService;
    private final GroupService groupService;
    private final UserService userService;
    private final EnrollmentService enrollmentService;

    @Autowired
    public LabJoinController(
            LabService labService,
            ClassService classService,
            GroupService groupService,
            UserService userService,
            EnrollmentService enrollmentService
    ) {
        this.labService = labService;
        this.classService = classService;
        this.groupService = groupService;
        this.userService = userService;
        this.enrollmentService = enrollmentService;
    }

    /**
     * Look up a lab roster using its join code (for the code entry screen).
     */
    @GetMapping("/join/{joinCode}")
    public ResponseEntity<?> getLabByJoinCode(@PathVariable String joinCode) {
        if (!StringUtils.hasText(joinCode)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Join code is required"));
        }

        Optional<Lab> labOpt = labService.getByJoinCode(joinCode);
        if (labOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Invalid or expired join code"));
        }

        Lab lab = labOpt.get();
        Optional<Class> classOpt = classService.getClassById(lab.getClassId());
        if (classOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Class for lab not found"));
        }

        return ResponseEntity.ok(buildLabResponse(lab, classOpt.get()));
    }

    /**
     * Join a lab by creating/finding a group for the student.
     */
    @PostMapping("/join/{joinCode}/students")
    public ResponseEntity<?> joinLab(
            @PathVariable String joinCode,
            @RequestBody StudentJoinRequest request
    ) {
        if (!StringUtils.hasText(joinCode)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Join code is required"));
        }
        if (request == null || !StringUtils.hasText(request.getStudentName())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Student name is required"));
        }

        Optional<Lab> labOpt = labService.getByJoinCode(joinCode);
        if (labOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Invalid or expired join code"));
        }
        Lab lab = labOpt.get();

        Optional<Class> classOpt = classService.getClassById(lab.getClassId());
        if (classOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Class for lab not found"));
        }
        Class classEntity = classOpt.get();

        String normalizedStudentName = request.getStudentName().trim();
        if (!rosterContainsStudent(classEntity, normalizedStudentName)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Student is not listed on this class roster"));
        }

        User studentUser = ensureStudentUser(normalizedStudentName, request.getStudentEmail());
        ensureStudentEnrollment(studentUser.getId(), classEntity.getId());

        Group group = ensureStudentGroup(lab, studentUser);

        return ResponseEntity.ok(new StudentJoinResponse(
                studentUser.getName(),
                buildLabResponse(lab, classEntity),
                new GroupSummary(group)
        ));
    }

    /**
     * Fetch a lab's detail (title, checkpoints, roster metadata) for student view refreshes.
     */
    @GetMapping("/{labId}")
    public ResponseEntity<?> getLabDetail(@PathVariable String labId) {
        if (!StringUtils.hasText(labId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lab ID is required"));
        }

        Optional<Lab> labOpt = labService.getById(labId);
        if (labOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Lab not found"));
        }

        Lab lab = labOpt.get();
        Optional<Class> classOpt = classService.getClassById(lab.getClassId());
        if (classOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Class for lab not found"));
        }

        return ResponseEntity.ok(buildLabResponse(lab, classOpt.get()));
    }

    /**
     * Fetch a single group (for student refresh/deep links).
     */
    @GetMapping("/{labId}/groups/{groupId}")
    public ResponseEntity<?> getGroupDetail(
            @PathVariable String labId,
            @PathVariable String groupId
    ) {
        if (!StringUtils.hasText(labId) || !StringUtils.hasText(groupId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lab ID and Group ID are required"));
        }

        Optional<Group> groupOpt = groupService.getById(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Group not found"));
        }

        Group group = groupOpt.get();
        if (!labId.equals(group.getLabId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Group does not belong to this lab"));
        }

        return ResponseEntity.ok(new GroupSummary(group));
    }

    private LabJoinResponse buildLabResponse(Lab lab, Class classEntity) {
        return new LabJoinResponse(lab, classEntity);
    }

    private boolean rosterContainsStudent(Class classEntity, String studentName) {
        List<String> roster = classEntity.getRoster();
        if (roster == null || roster.isEmpty()) {
            // Allow join even if roster is missing (e.g., manual classes)
            return true;
        }
        return roster.stream()
                .filter(StringUtils::hasText)
                .anyMatch(name -> name.equalsIgnoreCase(studentName));
    }

    private Group ensureStudentGroup(Lab lab, User studentUser) {
        List<Group> groups = groupService.getGroupsByLabId(lab.getId());
        Optional<Group> existing = groups.stream()
                .filter(group -> group.getMembers() != null && group.getMembers().stream()
                        .anyMatch(member ->
                                studentUser.getId().equals(member.getUserId()) ||
                                member.getName().equalsIgnoreCase(studentUser.getName())))
                .findFirst();

        if (existing.isPresent()) {
            Group group = existing.get();
            boolean hasMember = group.getMembers().stream()
                    .anyMatch(member -> member.getUserId().equals(studentUser.getId())
                            || member.getName().equalsIgnoreCase(studentUser.getName()));
            if (!hasMember) {
                group.getMembers().add(createGroupMember(studentUser));
            } else {
                // Sync existing member with canonical user info
                group.getMembers().forEach(member -> {
                    if (member.getName().equalsIgnoreCase(studentUser.getName())
                            || studentUser.getId().equals(member.getUserId())) {
                        member.setUserId(studentUser.getId());
                        member.setName(studentUser.getName());
                        member.setEmail(studentUser.getEmail());
                    }
                });
            }
            return groupService.upsert(group);
        }

        Group newGroup = new Group();
        newGroup.setLabId(lab.getId());
        newGroup.setGroupId(generateGroupDisplayId(studentUser.getName(), groups.size() + 1));
        newGroup.setGroupNumber(groups.size() + 1);
        newGroup.setStatus(GroupStatus.FORMING);

        List<GroupMember> members = new ArrayList<>();
        members.add(createGroupMember(studentUser));
        newGroup.setMembers(members);
        newGroup.setCheckpointProgress(new ArrayList<>());

        return groupService.upsert(newGroup);
    }

    private GroupMember createGroupMember(User user) {
        return new GroupMember(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    private String generateGroupDisplayId(String studentName, int fallbackNumber) {
        if (StringUtils.hasText(studentName)) {
            return studentName;
        }
        return "Group-" + fallbackNumber;
    }

    private User ensureStudentUser(String studentName, String email) {
        if (StringUtils.hasText(email)) {
            Optional<User> existingByEmail = userService.findByEmail(email.trim());
            if (existingByEmail.isPresent()) {
                return existingByEmail.get();
            }
        }

        String slug = slugify(studentName);
        String safeEmail = StringUtils.hasText(email)
                ? email.trim()
                : slug + "@students.local";

        User user = new User();
        user.setAuth0Id("local|" + UUID.randomUUID());
        user.setEmail(safeEmail);
        user.setName(studentName);
        user.setRoles(List.of("Student"));
        user.setPrimaryRole("Student");
        user.setCreatedAt(java.time.Instant.now());
        user.setLastLogin(java.time.Instant.now());

        if (StringUtils.hasText(studentName)) {
            String[] parts = studentName.trim().split("\\s+", 2);
            user.setFirstName(parts[0]);
            if (parts.length > 1) {
                user.setLastName(parts[1]);
            }
        }

        return userService.syncUserFromAuth0Data(user);
    }

    private Enrollment ensureStudentEnrollment(String userId, String classId) {
        Optional<Enrollment> existing = enrollmentService.getEnrollment(userId, classId);
        if (existing.isPresent()) {
            return existing.get();
        }
        return enrollmentService.enrollStudent(userId, classId);
    }

    private String slugify(String value) {
        if (!StringUtils.hasText(value)) {
            return "student";
        }
        return value.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    /**
     * Response payload describing the lab and roster.
     */
    private static class LabJoinResponse {
        private final String labId;
        private final String labCode;
        private final String labTitle;
        private final String labDescription;
        private final Integer labPoints;
        private final String classId;
        private final String className;
        private final List<String> students;
        private final List<CheckpointDefinition> checkpoints;

        LabJoinResponse(Lab lab, Class classEntity) {
            this.labId = lab.getId();
            this.labCode = lab.getJoinCode();
            this.labTitle = lab.getTitle();
            this.labDescription = lab.getDescription();
            this.labPoints = lab.getPoints();
            this.classId = classEntity.getId();
            this.className = classEntity.getCourseName();
            List<String> roster = classEntity.getRoster();
            this.students = roster != null ? roster : Collections.emptyList();
            List<CheckpointDefinition> defs = lab.getCheckpoints();
            this.checkpoints = defs != null ? defs : Collections.emptyList();
        }

        public String getLabId() {
            return labId;
        }

        public String getLabCode() {
            return labCode;
        }

        public String getLabTitle() {
            return labTitle;
        }

        public String getLabDescription() {
            return labDescription;
        }

        public Integer getLabPoints() {
            return labPoints;
        }

        public String getClassId() {
            return classId;
        }

        public String getClassName() {
            return className;
        }

        public List<String> getStudents() {
            return students;
        }

        public List<CheckpointDefinition> getCheckpoints() {
            return checkpoints;
        }
    }

    private static class StudentJoinRequest {
        private String studentName;
        private String studentEmail;

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public String getStudentEmail() {
            return studentEmail;
        }

        public void setStudentEmail(String studentEmail) {
            this.studentEmail = studentEmail;
        }
    }

    private static class StudentJoinResponse {
        private final String studentName;
        private final LabJoinResponse lab;
        private final GroupSummary group;

        StudentJoinResponse(String studentName, LabJoinResponse lab, GroupSummary group) {
            this.studentName = studentName;
            this.lab = lab;
            this.group = group;
        }

        public String getStudentName() {
            return studentName;
        }

        public LabJoinResponse getLab() {
            return lab;
        }

        public GroupSummary getGroup() {
            return group;
        }
    }

    private static class GroupSummary {
        private final String id;
        private final String groupId;
        private final GroupStatus status;
        private final List<GroupMember> members;
        private final List<CheckpointProgress> checkpointProgress;

        GroupSummary(Group group) {
            this.id = group.getId();
            this.groupId = group.getGroupId();
            this.status = group.getStatus();
            this.members = group.getMembers() != null ? group.getMembers() : Collections.emptyList();
            this.checkpointProgress = group.getCheckpointProgress() != null
                    ? group.getCheckpointProgress()
                    : Collections.emptyList();
        }

        public String getId() {
            return id;
        }

        public String getGroupId() {
            return groupId;
        }

        public GroupStatus getStatus() {
            return status;
        }

        public List<GroupMember> getMembers() {
            return members;
        }

        public List<CheckpointProgress> getCheckpointProgress() {
            return checkpointProgress;
        }
    }
}
