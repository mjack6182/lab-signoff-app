package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Class;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.service.ClassService;
import com.example.lab_signoff_backend.service.LabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Class/Course management
 * Handles class creation, roster management, and TA assignments
 */
@RestController
@RequestMapping("/api/classes")
@CrossOrigin(
        origins = {
                "http://localhost:5173",
                "http://localhost:5002",
                "https://lab-signoff-app.web.app",
                "https://lab-signoff-app.firebaseapp.com"
        },
        allowCredentials = "true"
)
public class ClassController {

    @Autowired
    private ClassService classService;

    @Autowired
    private LabService labService;

    /**
     * Create a new class
     * POST /api/classes
     */
    @PostMapping
    public ResponseEntity<Class> createClass(@RequestBody Class classEntity) {
        try {
            // Check if class already exists
            if (classService.classExists(
                    classEntity.getCourseCode(),
                    classEntity.getTerm(),
                    classEntity.getSection())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(null);
            }

            Class created = classService.createClass(classEntity);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all classes (optionally filter by instructor)
     * GET /api/classes?instructorId=xyz
     */
    @GetMapping
    public ResponseEntity<List<Class>> getClasses(
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String term,
            @RequestParam(required = false, defaultValue = "false") Boolean includeArchived) {
        try {
            List<Class> classes;

            if (instructorId != null && !instructorId.isEmpty()) {
                classes = classService.getClassesByInstructor(instructorId);
            } else if (term != null && !term.isEmpty()) {
                classes = classService.getClassesByTerm(term);
            } else {
                classes = classService.getAllActiveClasses();
            }

            // Filter out archived classes if not requested
            if (!includeArchived) {
                classes = classes.stream()
                        .filter(c -> !c.getArchived())
                        .toList();
            }

            return ResponseEntity.ok(classes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific class by ID
     * GET /api/classes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Class> getClassById(@PathVariable String id) {
        Optional<Class> classOpt = classService.getClassById(id);
        return classOpt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Import a new class (and associated labs) from a Canvas gradebook CSV
     * POST /api/classes/import
     */
    @PostMapping("/import")
    public ResponseEntity<?> importClassFromCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("instructorId") String instructorId,
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "term", required = false) String term,
            @RequestParam(value = "section", required = false) String section
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "CSV file is required"));
        }
        if (!StringUtils.hasText(instructorId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Instructor ID is required"));
        }

        String originalFilename = file.getOriginalFilename();
        String baseName = deriveNameFromFilename(originalFilename);

        String resolvedCourseName = StringUtils.hasText(courseName)
                ? courseName.trim()
                : baseName;

        String resolvedCourseCode = StringUtils.hasText(courseCode)
                ? courseCode.trim()
                : baseName.replaceAll("\\s+", "-").toUpperCase();

        String resolvedTerm = StringUtils.hasText(term)
                ? term.trim()
                : "Imported " + Year.now();

        String resolvedSection = StringUtils.hasText(section)
                ? section.trim()
                : null;

        Class createdClass = null;
        try {
            Class newClass = new Class(resolvedCourseCode, resolvedCourseName, resolvedTerm, instructorId);
            if (resolvedSection != null) {
                newClass.setSection(resolvedSection);
            }

            createdClass = classService.createClass(newClass);
            Class populatedClass = classService.importRosterFromCsv(createdClass.getId(), file);

            return ResponseEntity.status(HttpStatus.CREATED).body(populatedClass);
        } catch (Exception e) {
            if (createdClass != null && createdClass.getId() != null) {
                classService.deleteClass(createdClass.getId());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Failed to import class: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all labs for a specific class
     * GET /api/classes/{classId}/labs
     */
    @GetMapping("/{classId}/labs")
    public ResponseEntity<List<Lab>> getLabsByClassId(@PathVariable String classId) {
        // Verify class exists
        Optional<Class> classOpt = classService.getClassById(classId);
        if (classOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Fetch all labs for this class
        List<Lab> labs = labService.getLabsByClassId(classId);
        return ResponseEntity.ok(labs);
    }

    /**
     * Update a class
     * PUT /api/classes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Class> updateClass(
            @PathVariable String id,
            @RequestBody Class classEntity) {
        try {
            Class updated = classService.updateClass(id, classEntity);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a class
     * DELETE /api/classes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClass(@PathVariable String id) {
        try {
            classService.deleteClass(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Archive a class
     * PUT /api/classes/{id}/archive
     */
    @PutMapping("/{id}/archive")
    public ResponseEntity<Class> archiveClass(@PathVariable String id) {
        try {
            Class archived = classService.archiveClass(id);
            return ResponseEntity.ok(archived);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Import roster from Canvas CSV
     * POST /api/classes/{id}/roster/import
     */
    @PostMapping("/{id}/roster/import")
    public ResponseEntity<Class> importRoster(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Class updated = classService.importRosterFromCsv(id, file);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Add a student to the roster
     * POST /api/classes/{id}/roster/students/{studentId}
     */
    @PostMapping("/{id}/roster/students/{studentId}")
    public ResponseEntity<Class> addStudentToRoster(
            @PathVariable String id,
            @PathVariable String studentId) {
        try {
            Class updated = classService.addStudentToRoster(id, studentId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Remove a student from the roster
     * DELETE /api/classes/{id}/roster/students/{studentId}
     */
    @DeleteMapping("/{id}/roster/students/{studentId}")
    public ResponseEntity<Class> removeStudentFromRoster(
            @PathVariable String id,
            @PathVariable String studentId) {
        try {
            Class updated = classService.removeStudentFromRoster(id, studentId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assign a TA to the class
     * POST /api/classes/{id}/tas/{userId}
     */
    @PostMapping("/{id}/tas/{userId}")
    public ResponseEntity<Class> assignTA(
            @PathVariable String id,
            @PathVariable String userId) {
        try {
            Class updated = classService.assignTA(id, userId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Remove a TA from the class
     * DELETE /api/classes/{id}/tas/{userId}
     */
    @DeleteMapping("/{id}/tas/{userId}")
    public ResponseEntity<Class> removeTA(
            @PathVariable String id,
            @PathVariable String userId) {
        try {
            Class updated = classService.removeTA(id, userId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if a student is in the roster
     * GET /api/classes/{id}/roster/students/{studentId}/check
     */
    @GetMapping("/{id}/roster/students/{studentId}/check")
    public ResponseEntity<Map<String, Boolean>> checkStudentInRoster(
            @PathVariable String id,
            @PathVariable String studentId) {
        boolean inRoster = classService.isStudentInRoster(id, studentId);
        return ResponseEntity.ok(Map.of("inRoster", inRoster));
    }

    /**
     * Check if a user is a TA
     * GET /api/classes/{id}/tas/{userId}/check
     */
    @GetMapping("/{id}/tas/{userId}/check")
    public ResponseEntity<Map<String, Boolean>> checkIsTA(
            @PathVariable String id,
            @PathVariable String userId) {
        boolean isTA = classService.isTA(id, userId);
        return ResponseEntity.ok(Map.of("isTA", isTA));
    }

    /**
     * Check if a user is staff (instructor or TA)
     * GET /api/classes/{id}/staff/{userId}/check
     */
    @GetMapping("/{id}/staff/{userId}/check")
    public ResponseEntity<Map<String, Boolean>> checkIsStaff(
            @PathVariable String id,
            @PathVariable String userId) {
        boolean isStaff = classService.isStaff(id, userId);
        return ResponseEntity.ok(Map.of("isStaff", isStaff));
    }

    private String deriveNameFromFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "Imported Class";
        }

        String base = filename;
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            base = filename.substring(0, dotIndex);
        }

        String cleaned = base.replace('_', ' ').replace('-', ' ').trim();
        return StringUtils.hasText(cleaned) ? cleaned : "Imported Class";
    }
}
