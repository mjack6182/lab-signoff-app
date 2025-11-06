package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Enrollment;
import com.example.lab_signoff_backend.model.enums.EnrollmentRole;
import com.example.lab_signoff_backend.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Enrollment management
 * Handles user enrollments, role changes, and enrollment status
 */
@RestController
@RequestMapping("/api/enrollments")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    /**
     * Create a new enrollment
     * POST /api/enrollments
     * Body: { "userId": "xyz", "classId": "abc", "role": "STUDENT" }
     */
    @PostMapping
    public ResponseEntity<Enrollment> createEnrollment(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String classId = request.get("classId");
            String roleStr = request.getOrDefault("role", "STUDENT");

            EnrollmentRole role = EnrollmentRole.valueOf(roleStr);

            Enrollment enrollment = enrollmentService.enrollUser(userId, classId, role);
            return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            // Already enrolled
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get enrollment by ID
     * GET /api/enrollments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Enrollment> getEnrollment(@PathVariable String id) {
        Optional<Enrollment> enrollment = enrollmentService.getEnrollmentById(id);
        return enrollment.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all enrollments for a user
     * GET /api/enrollments/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Enrollment>> getEnrollmentsByUser(
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        try {
            List<Enrollment> enrollments = activeOnly
                    ? enrollmentService.getActiveEnrollmentsByUser(userId)
                    : enrollmentService.getEnrollmentsByUser(userId);
            return ResponseEntity.ok(enrollments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all enrollments for a class (roster)
     * GET /api/enrollments/class/{classId}
     */
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<Enrollment>> getEnrollmentsByClass(
            @PathVariable String classId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        try {
            List<Enrollment> enrollments;

            if (role != null && !role.isEmpty()) {
                EnrollmentRole enrollmentRole = EnrollmentRole.valueOf(role);
                enrollments = activeOnly
                        ? enrollmentService.getActiveStudents(classId) // or TAs depending on role
                        : enrollmentService.getStudents(classId);
            } else {
                enrollments = enrollmentService.getEnrollmentsByClass(classId);
            }

            return ResponseEntity.ok(enrollments);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get students for a class
     * GET /api/enrollments/class/{classId}/students
     */
    @GetMapping("/class/{classId}/students")
    public ResponseEntity<List<Enrollment>> getStudents(
            @PathVariable String classId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        try {
            List<Enrollment> students = activeOnly
                    ? enrollmentService.getActiveStudents(classId)
                    : enrollmentService.getStudents(classId);
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get TAs for a class
     * GET /api/enrollments/class/{classId}/tas
     */
    @GetMapping("/class/{classId}/tas")
    public ResponseEntity<List<Enrollment>> getTAs(
            @PathVariable String classId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        try {
            List<Enrollment> tas = activeOnly
                    ? enrollmentService.getActiveTAs(classId)
                    : enrollmentService.getTAs(classId);
            return ResponseEntity.ok(tas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get staff (teachers and TAs) for a class
     * GET /api/enrollments/class/{classId}/staff
     */
    @GetMapping("/class/{classId}/staff")
    public ResponseEntity<List<Enrollment>> getStaff(@PathVariable String classId) {
        try {
            List<Enrollment> staff = enrollmentService.getStaff(classId);
            return ResponseEntity.ok(staff);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upgrade a student to TA
     * PUT /api/enrollments/{id}/upgrade
     * Body: { "performedBy": "teacherUserId" }
     */
    @PutMapping("/{id}/upgrade")
    public ResponseEntity<Enrollment> upgradeToTA(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            String performedBy = request.get("performedBy");
            if (performedBy == null || performedBy.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Enrollment updated = enrollmentService.upgradeToTA(id, performedBy);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Change enrollment role
     * PUT /api/enrollments/{id}/role
     * Body: { "role": "TA", "performedBy": "teacherUserId" }
     */
    @PutMapping("/{id}/role")
    public ResponseEntity<Enrollment> changeRole(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            String roleStr = request.get("role");
            String performedBy = request.get("performedBy");

            if (roleStr == null || performedBy == null) {
                return ResponseEntity.badRequest().build();
            }

            EnrollmentRole role = EnrollmentRole.valueOf(roleStr);
            Enrollment updated = enrollmentService.changeRole(id, role, performedBy);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Drop an enrollment
     * PUT /api/enrollments/{id}/drop
     */
    @PutMapping("/{id}/drop")
    public ResponseEntity<Enrollment> dropEnrollment(@PathVariable String id) {
        try {
            Enrollment updated = enrollmentService.dropEnrollment(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Complete an enrollment (end of term)
     * PUT /api/enrollments/{id}/complete
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<Enrollment> completeEnrollment(@PathVariable String id) {
        try {
            Enrollment updated = enrollmentService.completeEnrollment(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete an enrollment
     * DELETE /api/enrollments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable String id) {
        try {
            enrollmentService.deleteEnrollment(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if a user is enrolled in a class
     * GET /api/enrollments/check?userId=xyz&classId=abc
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkEnrollment(
            @RequestParam String userId,
            @RequestParam String classId) {
        boolean isEnrolled = enrollmentService.isEnrolled(userId, classId);
        Optional<Enrollment> enrollment = enrollmentService.getEnrollment(userId, classId);

        return ResponseEntity.ok(Map.of(
                "isEnrolled", isEnrolled,
                "enrollment", enrollment.orElse(null)
        ));
    }

    /**
     * Get enrollment statistics for a class
     * GET /api/enrollments/class/{classId}/stats
     */
    @GetMapping("/class/{classId}/stats")
    public ResponseEntity<Map<String, Long>> getEnrollmentStats(@PathVariable String classId) {
        try {
            long studentCount = enrollmentService.countStudents(classId);
            long taCount = enrollmentService.countTAs(classId);

            return ResponseEntity.ok(Map.of(
                    "students", studentCount,
                    "tas", taCount,
                    "total", studentCount + taCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
