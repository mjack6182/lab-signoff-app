package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Class;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.enums.LabStatus;
import com.example.lab_signoff_backend.repository.ClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream()))) {
            String headerLine = reader.readLine();
            String pointsLine = reader.readLine();

            if (headerLine == null || pointsLine == null) {
                throw new RuntimeException("CSV file must have at least 2 rows (header and points possible)");
            }

            // Parse headers to find lab columns
            String[] headers = headerLine.split(",");
            String[] points = pointsLine.split(",");

            // Pattern to match lab assignments like "Laboratory for Module 01 (9601577)"
            Pattern labPattern = Pattern.compile("Laboratory for (.+?)\\s*\\((\\d+)\\)");

            // Parse lab columns from headers (starting after the first 4 student info columns)
            for (int i = 4; i < headers.length; i++) {
                String header = headers[i].trim();
                Matcher matcher = labPattern.matcher(header);

                if (matcher.find()) {
                    String labTitle = "Laboratory for " + matcher.group(1);
                    String canvasId = matcher.group(2);

                    // Get points from Points Possible row
                    Integer labPoints = 1; // Default to 1 point if parsing fails
                    try {
                        if (i < points.length && !points[i].trim().isEmpty()) {
                            labPoints = (int) Double.parseDouble(points[i].trim());
                        }
                    } catch (NumberFormatException e) {
                        // Use default value of 1
                    }

                    // Create Lab entity
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

            // Parse student roster (rows 3+)
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", -1);

                // Canvas CSV format: Student,ID,SIS User ID,SIS Login ID,...
                if (columns.length >= 4) {
                    String studentName = columns[0].trim();
                    // Skip empty rows or summary rows
                    if (studentName.isEmpty() || studentName.equals("Student, Test")) {
                        continue;
                    }

                    rosterEntries.add(studentName);
                }
            }

            // Add all students to roster
            for (String entry : rosterEntries) {
                classEntity.addStudentToRoster(entry);
            }

            return classRepository.save(classEntity);

        } catch (Exception e) {
            throw new RuntimeException("Failed to import roster from CSV: " + e.getMessage(), e);
        }
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
}
