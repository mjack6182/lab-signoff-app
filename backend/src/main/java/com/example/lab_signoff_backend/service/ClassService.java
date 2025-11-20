package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Class;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.enums.LabStatus;
import com.example.lab_signoff_backend.repository.ClassRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service class for Class/Course management operations
 */
@Service
public class ClassService {

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private LabService labService;

    /**
     * Create a new class
     */
    public Class createClass(Class classEntity) {
        return classRepository.save(classEntity);
    }

    /**
     * Get class by ID
     */
    public Optional<Class> getClassById(String id) {
        return classRepository.findById(id);
    }

    /**
     * Get all classes for an instructor
     */
    public List<Class> getClassesByInstructor(String instructorId) {
        return classRepository.findByInstructorId(instructorId);
    }

    /**
     * Get all active (non-archived) classes for an instructor
     */
    public List<Class> getActiveClassesByInstructor(String instructorId) {
        return classRepository.findByInstructorId(instructorId)
                .stream()
                .filter(c -> !c.getArchived())
                .toList();
    }

    /**
     * Get classes by term
     */
    public List<Class> getClassesByTerm(String term) {
        return classRepository.findByTerm(term);
    }

    /**
     * Get all active classes
     */
    public List<Class> getAllActiveClasses() {
        return classRepository.findByArchivedFalse();
    }

    /**
     * Update a class
     */
    public Class updateClass(String id, Class updates) {
        Optional<Class> classOpt = classRepository.findById(id);
        if (classOpt.isEmpty()) {
            throw new RuntimeException("Class not found with id: " + id);
        }

        Class classEntity = classOpt.get();

        if (updates.getCourseName() != null) {
            classEntity.setCourseName(updates.getCourseName());
        }
        if (updates.getCourseCode() != null) {
            classEntity.setCourseCode(updates.getCourseCode());
        }
        if (updates.getSection() != null) {
            classEntity.setSection(updates.getSection());
        }
        if (updates.getTerm() != null) {
            classEntity.setTerm(updates.getTerm());
        }
        if (updates.getRoster() != null) {
            classEntity.setRoster(new ArrayList<>(updates.getRoster()));
        }

        classEntity.updateTimestamp();
        return classRepository.save(classEntity);
    }

    /**
     * Delete a class
     */
    public void deleteClass(String id) {
        classRepository.deleteById(id);
    }

    /**
     * Archive a class
     */
    public Class archiveClass(String id) {
        Optional<Class> classOpt = classRepository.findById(id);
        if (classOpt.isPresent()) {
            Class classEntity = classOpt.get();
            classEntity.setArchived(true);
            classEntity.updateTimestamp();
            return classRepository.save(classEntity);
        }
        throw new RuntimeException("Class not found with id: " + id);
    }

    /**
     * Add a student to the class roster
     */
    public Class addStudentToRoster(String classId, String studentId) {
        Optional<Class> classOpt = classRepository.findById(classId);
        if (classOpt.isPresent()) {
            Class classEntity = classOpt.get();
            classEntity.addStudentToRoster(studentId);
            return classRepository.save(classEntity);
        }
        throw new RuntimeException("Class not found with id: " + classId);
    }

    /**
     * Remove a student from the class roster
     */
    public Class removeStudentFromRoster(String classId, String studentId) {
        Optional<Class> classOpt = classRepository.findById(classId);
        if (classOpt.isPresent()) {
            Class classEntity = classOpt.get();
            classEntity.removeStudentFromRoster(studentId);
            return classRepository.save(classEntity);
        }
        throw new RuntimeException("Class not found with id: " + classId);
    }

    /**
     * Assign a TA to the class
     */
    public Class assignTA(String classId, String userId) {
        Optional<Class> classOpt = classRepository.findById(classId);
        if (classOpt.isPresent()) {
            Class classEntity = classOpt.get();
            classEntity.addTA(userId);
            return classRepository.save(classEntity);
        }
        throw new RuntimeException("Class not found with id: " + classId);
    }

    /**
     * Remove a TA from the class
     */
    public Class removeTA(String classId, String userId) {
        Optional<Class> classOpt = classRepository.findById(classId);
        if (classOpt.isPresent()) {
            Class classEntity = classOpt.get();
            classEntity.removeTA(userId);
            return classRepository.save(classEntity);
        }
        throw new RuntimeException("Class not found with id: " + classId);
    }

    /**
     * Import roster and labs from Canvas CSV export
     * Parses the CSV and adds students to the roster and creates lab assignments
     */
    public Class importRosterFromCsv(String classId, MultipartFile csvFile) {
        Optional<Class> classOpt = classRepository.findById(classId);
        if (classOpt.isEmpty()) {
            throw new RuntimeException("Class not found with id: " + classId);
        }

        Class classEntity = classOpt.get();
        List<String> rosterEntries = new ArrayList<>();
        List<Lab> labsToCreate = new ArrayList<>();

        try (InputStreamReader reader = new InputStreamReader(csvFile.getInputStream());
             CSVParser parser = new CSVParser(reader, CSVFormat.RFC4180.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true) // consume header row instead of returning it as a record
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build())) {

            List<String> headers = parser.getHeaderNames();
            if (headers == null || headers.isEmpty()) {
                throw new RuntimeException("CSV file missing header row");
            }

            // Map normalized header -> actual header name for case-insensitive/BOM tolerant lookups
            Map<String, String> headerLookup = new HashMap<>();
            for (String h : headers) {
                headerLookup.putIfAbsent(normalizeHeader(h), h);
            }

            String studentHeader = headerLookup.getOrDefault("student", headers.get(0));

            // Pattern to match lab assignments like "Laboratory for Module 01 (9601577)"
            Pattern labPattern = Pattern.compile("Laboratory for (.+?)\\s*\\((\\d+)\\)");

            List<CSVRecord> records = parser.getRecords();
            CSVRecord pointsRecord = records.stream()
                    .filter(r -> {
                        String studentCol = getValue(r, studentHeader, headerLookup);
                        return studentCol != null && studentCol.toLowerCase().contains("points possible");
                    })
                    .findFirst()
                    .orElse(null);

            // Build labs from header definitions
            for (int i = 4; i < headers.size(); i++) {
                String header = headers.get(i).trim();
                Matcher matcher = labPattern.matcher(header);

                if (matcher.find()) {
                    String labTitle = "Laboratory for " + matcher.group(1);

                    Integer labPoints = 1; // Default to 1 point if parsing fails
                    if (pointsRecord != null && i < pointsRecord.size()) {
                        String pointsValue = pointsRecord.get(i);
                        try {
                            if (pointsValue != null && !pointsValue.trim().isEmpty()) {
                                labPoints = (int) Double.parseDouble(pointsValue.trim());
                            }
                        } catch (NumberFormatException ignored) {
                            // keep default
                        }
                    }

                    Lab lab = new Lab(
                            classId,
                            labTitle,
                            labPoints,
                            classEntity.getInstructorId()
                    );
                    lab.setDescription("Imported from Canvas: " + header);
                    lab.setStatus(LabStatus.DRAFT); // Start as draft, teacher can activate later

                    labsToCreate.add(lab);
                }
            }

            // Save all labs
            for (Lab lab : labsToCreate) {
                labService.upsert(lab);
            }

            // Parse student roster records (skip points row and empty Student values)
            for (CSVRecord record : records) {
                String studentName = getValue(record, studentHeader, headerLookup);
                if (studentName == null) {
                    continue;
                }
                String normalized = studentName.trim();
                if (normalized.isEmpty() || normalized.toLowerCase().contains("points possible")) {
                    continue;
                }
                rosterEntries.add(normalized);
            }

            for (String entry : rosterEntries) {
                classEntity.addStudentToRoster(entry);
            }

            return classRepository.save(classEntity);

        } catch (Exception e) {
            throw new RuntimeException("Failed to import roster from CSV: " + e.getMessage(), e);
        }
    }

    private String getValue(CSVRecord record, String logicalHeader, Map<String, String> headerLookup) {
        if (record == null || logicalHeader == null) {
            return null;
        }
        String key = headerLookup.get(normalizeHeader(logicalHeader));
        if (key != null && record.isMapped(key)) {
            return record.get(key);
        }
        // Fallback: try any header that normalizes to the same value
        for (var entry : headerLookup.entrySet()) {
            if (normalizeHeader(logicalHeader).equals(entry.getKey()) && record.isMapped(entry.getValue())) {
                return record.get(entry.getValue());
            }
        }
        return null;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.replace("\uFEFF", "").trim().toLowerCase();
    }

    /**
     * Check if a student is in the class roster
     */
    public boolean isStudentInRoster(String classId, String studentId) {
        Optional<Class> classOpt = classRepository.findById(classId);
        return classOpt.map(c -> c.isStudentInRoster(studentId)).orElse(false);
    }

    /**
     * Check if a user is a TA for the class
     */
    public boolean isTA(String classId, String userId) {
        Optional<Class> classOpt = classRepository.findById(classId);
        return classOpt.map(c -> c.isTA(userId)).orElse(false);
    }

    /**
     * Check if a user is the instructor for the class
     */
    public boolean isInstructor(String classId, String userId) {
        Optional<Class> classOpt = classRepository.findById(classId);
        return classOpt.map(c -> c.isInstructor(userId)).orElse(false);
    }

    /**
     * Check if a user is staff (instructor or TA) for the class
     */
    public boolean isStaff(String classId, String userId) {
        Optional<Class> classOpt = classRepository.findById(classId);
        return classOpt.map(c -> c.isStaff(userId)).orElse(false);
    }

    /**
     * Find class by course code, term, and section
     */
    public Optional<Class> findByClassInfo(String courseCode, String term, String section) {
        return classRepository.findByCourseCodeAndTermAndSection(courseCode, term, section);
    }

    /**
     * Check if a class exists with the given course code, term, and section
     */
    public boolean classExists(String courseCode, String term, String section) {
        return classRepository.existsByCourseCodeAndTermAndSection(courseCode, term, section);
    }

    private String unquote(String value) {
        if (value == null) {
            return "";
        }
        String result = value;
        if (result.length() >= 2 && result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length() - 1).replace("\"\"", "\"");
        }
        return result;
    }

    private List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return columns;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                columns.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        columns.add(current.toString());
        return columns;
    }
}
