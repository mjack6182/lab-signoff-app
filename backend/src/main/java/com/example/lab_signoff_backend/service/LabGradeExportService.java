package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Class;
import com.example.lab_signoff_backend.model.Enrollment;
import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.User;
import com.example.lab_signoff_backend.model.embedded.CheckpointDefinition;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Generates Canvas-compatible CSV exports for individual labs.
 */
@Service
public class LabGradeExportService {
    private static final String IMPORTED_PREFIX = "Imported from Canvas:";

    private final LabService labService;
    private final ClassService classService;
    private final GroupService groupService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    public LabGradeExportService(
            LabService labService,
            ClassService classService,
            GroupService groupService,
            EnrollmentService enrollmentService,
            UserService userService
    ) {
        this.labService = labService;
        this.classService = classService;
        this.groupService = groupService;
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

    /**
     * Build a CSV export for a given lab.
     *
     * @param labId The lab identifier
     * @return ExportResult containing filename and bytes
     */
    public ExportResult generateCsv(String labId) {
        Lab lab = labService.getById(labId)
                .orElseThrow(() -> new NoSuchElementException("Lab not found: " + labId));
        Class classEntity = classService.getClassById(lab.getClassId())
                .orElseThrow(() -> new NoSuchElementException("Class not found for lab " + labId));

        List<Group> groups = groupService.getGroupsByLabId(labId);
        List<Enrollment> enrollments = enrollmentService.getActiveStudents(classEntity.getId());
        Set<String> enrollmentUserIds = enrollments.stream()
                .map(Enrollment::getUserId)
                .collect(Collectors.toSet());
        Map<String, User> users = userService.findByIds(enrollmentUserIds);

        Map<Integer, Integer> checkpointPoints = buildCheckpointPointMap(lab);
        BigDecimal pointsPossible = calculatePointsPossible(checkpointPoints, lab);

        RowSet rowSet = buildInitialRows(classEntity, enrollments, users);
        applyGroupResults(
                groups,
                checkpointPoints,
                pointsPossible,
                rowSet.orderedRows,
                rowSet.rowsByUserId,
                users,
                classEntity
        );
        finalizeRosterZeros(rowSet.orderedRows.values());

        byte[] csvBytes = writeCsv(
                determineCanvasColumnName(lab),
                pointsPossible,
                rowSet.orderedRows.values()
        );

        return new ExportResult("lab_" + labId + "_grades.csv", csvBytes);
    }

    private Map<Integer, Integer> buildCheckpointPointMap(Lab lab) {
        List<CheckpointDefinition> definitions = lab.getCheckpoints();
        if (definitions == null || definitions.isEmpty()) {
            Map<Integer, Integer> fallback = new LinkedHashMap<>();
            Integer totalPoints = lab.getPoints();
            if (totalPoints != null && totalPoints > 0) {
                for (int i = 1; i <= totalPoints; i++) {
                    fallback.put(i, 1);
                }
            }
            return fallback;
        }

        Map<Integer, Integer> points = new LinkedHashMap<>();
        for (CheckpointDefinition def : definitions) {
            if (def == null || def.getNumber() == null) {
                continue;
            }
            int value = (def.getPoints() != null && def.getPoints() > 0) ? def.getPoints() : 1;
            points.putIfAbsent(def.getNumber(), value);
        }
        return points;
    }

    private BigDecimal calculatePointsPossible(Map<Integer, Integer> checkpointPoints, Lab lab) {
        BigDecimal total = checkpointPoints.values().stream()
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            Integer fallback = lab.getPoints();
            if (fallback != null && fallback > 0) {
                total = BigDecimal.valueOf(fallback);
            }
        }
        return total.compareTo(BigDecimal.ZERO) > 0 ? total : BigDecimal.ONE;
    }

    private RowSet buildInitialRows(Class classEntity,
                                    List<Enrollment> enrollments,
                                    Map<String, User> users) {
        Map<String, StudentRow> rowsByUserId = new LinkedHashMap<>();
        String defaultSection = defaultSection(classEntity);

        for (Enrollment enrollment : enrollments) {
            String userId = enrollment.getUserId();
            StudentRow row = new StudentRow("user::" + userId);
            row.userId = userId;
            row.section = defaultSection;
            populateFromUser(row, users.get(userId));
            rowsByUserId.put(userId, row);
        }

        LinkedHashMap<String, StudentRow> orderedRows = new LinkedHashMap<>();
        int rosterIndex = 0;
        List<String> roster = classEntity.getRoster() != null
                ? classEntity.getRoster()
                : Collections.emptyList();
        for (String entry : roster) {
            String trimmed = safeTrim(entry);
            if (trimmed.isEmpty()) {
                continue;
            }
            StudentRow row = rowsByUserId.get(trimmed);
            if (row == null) {
                row = findByNormalizedName(rowsByUserId.values(), normalizeName(trimmed));
            }
            if (row != null) {
                row.onRoster = true;
                orderedRows.put(row.key, row);
                continue;
            }
            StudentRow rosterRow = new StudentRow("roster::" + rosterIndex++);
            rosterRow.onRoster = true;
            rosterRow.section = defaultSection;
            rosterRow.studentName = trimmed;
            rosterRow.normalizedName = normalizeName(trimmed);
            orderedRows.put(rosterRow.key, rosterRow);
        }

        for (StudentRow row : rowsByUserId.values()) {
            if (!orderedRows.containsKey(row.key)) {
                orderedRows.put(row.key, row);
            }
        }

        return new RowSet(orderedRows, rowsByUserId);
    }

    private void applyGroupResults(List<Group> groups,
                                   Map<Integer, Integer> checkpointPoints,
                                   BigDecimal pointsPossible,
                                   LinkedHashMap<String, StudentRow> orderedRows,
                                   Map<String, StudentRow> rowsByUserId,
                                   Map<String, User> users,
                                   Class classEntity) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        String defaultSection = defaultSection(classEntity);
        AtomicInteger extraCounter = new AtomicInteger(0);

        for (Group group : groups) {
            BigDecimal groupScore = clampScore(
                    calculateGroupScore(group, checkpointPoints),
                    pointsPossible
            );
            if (group.getMembers() == null) {
                continue;
            }

            for (GroupMember member : group.getMembers()) {
                if (member == null) {
                    continue;
                }
                StudentRow row = null;
                String memberUserId = member.getUserId();
                if (memberUserId != null) {
                    row = rowsByUserId.get(memberUserId);
                    if (row == null) {
                        row = new StudentRow("user::" + memberUserId);
                        row.userId = memberUserId;
                        row.section = defaultSection;
                        rowsByUserId.put(memberUserId, row);
                    }
                }

                if (row == null && StringUtils.hasText(member.getName())) {
                    row = findByNormalizedName(
                            orderedRows.values(),
                            normalizeName(member.getName())
                    );
                }

                if (row == null) {
                    row = new StudentRow("group::" + extraCounter.getAndIncrement());
                    row.section = defaultSection;
                }

                if (!orderedRows.containsKey(row.key)) {
                    orderedRows.put(row.key, row);
                }

                if (!StringUtils.hasText(row.studentName)) {
                    row.studentName = determineDisplayName(member, users.get(memberUserId));
                    row.normalizedName = normalizeName(row.studentName);
                }
                if (!StringUtils.hasText(row.id)) {
                    row.id = memberUserId != null ? memberUserId : "";
                }
                if (!StringUtils.hasText(row.sisLoginId) && StringUtils.hasText(member.getEmail())) {
                    row.sisLoginId = member.getEmail();
                }
                if (!StringUtils.hasText(row.section)) {
                    row.section = defaultSection;
                }

                boolean present = member.getPresent() == null || Boolean.TRUE.equals(member.getPresent());
                row.score = present ? groupScore : BigDecimal.ZERO;
                row.hasScore = true;
            }
        }
    }

    private BigDecimal calculateGroupScore(Group group, Map<Integer, Integer> checkpointPoints) {
        if (group.getCheckpointProgress() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (CheckpointProgress progress : group.getCheckpointProgress()) {
            if (progress == null) {
                continue;
            }
            if (progress.getPointsAwarded() != null) {
                total = total.add(BigDecimal.valueOf(progress.getPointsAwarded()));
                continue;
            }
            if (progress.getStatus() == SignoffAction.PASS) {
                int points = checkpointPoints.getOrDefault(progress.getCheckpointNumber(), 1);
                total = total.add(BigDecimal.valueOf(points));
            }
        }
        return total;
    }

    private void finalizeRosterZeros(Collection<StudentRow> rows) {
        for (StudentRow row : rows) {
            if (row.onRoster && !row.hasScore) {
                row.score = BigDecimal.ZERO;
                row.hasScore = true;
            }
        }
    }

    private byte[] writeCsv(String labColumnName,
                            BigDecimal pointsPossible,
                            Collection<StudentRow> rows) {
        try (StringWriter writer = new StringWriter();
             CSVPrinter csv = new CSVPrinter(writer, CSVFormat.RFC4180)) {

            csv.printRecord("Student", "ID", "SIS User ID", "SIS Login ID", "Section", labColumnName);
            csv.printRecord(
                    "Points Possible",
                    "",
                    "",
                    "",
                    "",
                    formatDecimal(pointsPossible)
            );

            for (StudentRow row : rows) {
                csv.printRecord(
                        safe(row.studentName),
                        safe(row.id),
                        safe(row.sisUserId),
                        safe(row.sisLoginId),
                        safe(row.section),
                        gradeCell(row)
                );
            }

            csv.flush();
            return writer.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate CSV export", e);
        }
    }

    private String determineCanvasColumnName(Lab lab) {
        String description = lab.getDescription();
        if (StringUtils.hasText(description)) {
            String trimmed = description.trim();
            if (trimmed.startsWith(IMPORTED_PREFIX)) {
                trimmed = trimmed.substring(IMPORTED_PREFIX.length()).trim();
            }
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return StringUtils.hasText(lab.getTitle()) ? lab.getTitle() : "Lab " + lab.getId();
    }

    private void populateFromUser(StudentRow row, User user) {
        if (user == null) {
            row.studentName = StringUtils.hasText(row.studentName) ? row.studentName : "Unknown Student";
            row.normalizedName = normalizeName(row.studentName);
            row.id = StringUtils.hasText(row.id) ? row.id : safe(row.userId);
            row.sisUserId = safe(row.sisUserId);
            row.sisLoginId = safe(row.sisLoginId);
            return;
        }

        row.studentName = buildCanvasName(user);
        row.normalizedName = normalizeName(row.studentName);
        row.id = safe(user.getId());
        row.sisUserId = safe(user.getAuth0Id());
        row.sisLoginId = safe(user.getEmail());
    }

    private String buildCanvasName(User user) {
        if (user == null) {
            return "Unknown Student";
        }
        if (StringUtils.hasText(user.getLastName()) && StringUtils.hasText(user.getFirstName())) {
            return user.getLastName().trim() + ", " + user.getFirstName().trim();
        }
        if (StringUtils.hasText(user.getName())) {
            return user.getName();
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail();
        }
        return "Student " + safe(user.getId());
    }

    private StudentRow findByNormalizedName(Collection<StudentRow> rows, String normalized) {
        if (normalized.isEmpty()) {
            return null;
        }
        for (StudentRow row : rows) {
            if (normalized.equals(row.normalizedName)) {
                return row;
            }
        }
        return null;
    }

    private String determineDisplayName(GroupMember member, User user) {
        if (member != null && StringUtils.hasText(member.getName())) {
            return member.getName();
        }
        return buildCanvasName(user);
    }

    private BigDecimal clampScore(BigDecimal score, BigDecimal max) {
        if (score == null) {
            return BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (max != null && score.compareTo(max) > 0) {
            return max;
        }
        return score;
    }

    private String gradeCell(StudentRow row) {
        if (row.score == null) {
            return "";
        }
        return formatDecimal(row.score);
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private String defaultSection(Class classEntity) {
        return classEntity.getSection() != null ? classEntity.getSection() : "";
    }

    /**
     * DTO describing generated CSV bytes.
     */
    public static class ExportResult {
        private final String fileName;
        private final byte[] content;

        public ExportResult(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getContent() {
            return content;
        }
    }

    private static class RowSet {
        private final LinkedHashMap<String, StudentRow> orderedRows;
        private final Map<String, StudentRow> rowsByUserId;

        RowSet(LinkedHashMap<String, StudentRow> orderedRows, Map<String, StudentRow> rowsByUserId) {
            this.orderedRows = orderedRows;
            this.rowsByUserId = rowsByUserId;
        }
    }

    private static class StudentRow {
        private final String key;
        private String userId;
        private String studentName;
        private String normalizedName;
        private String id;
        private String sisUserId;
        private String sisLoginId;
        private String section;
        private boolean onRoster;
        private boolean hasScore;
        private BigDecimal score;

        StudentRow(String key) {
            this.key = key;
        }
    }
}
