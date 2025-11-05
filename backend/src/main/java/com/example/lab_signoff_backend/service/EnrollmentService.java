package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Enrollment;
import com.example.lab_signoff_backend.model.enums.EnrollmentRole;
import com.example.lab_signoff_backend.model.enums.EnrollmentStatus;
import com.example.lab_signoff_backend.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service class for Enrollment management operations
 */
@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Create a new enrollment
     */
    public Enrollment createEnrollment(Enrollment enrollment) {
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Enroll a student in a class
     */
    public Enrollment enrollStudent(String userId, String classId) {
        // Check if already enrolled
        if (enrollmentRepository.existsByUserIdAndClassId(userId, classId)) {
            throw new RuntimeException("User is already enrolled in this class");
        }

        Enrollment enrollment = new Enrollment(userId, classId, EnrollmentRole.STUDENT);
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Enroll a user with a specific role
     */
    public Enrollment enrollUser(String userId, String classId, EnrollmentRole role) {
        if (enrollmentRepository.existsByUserIdAndClassId(userId, classId)) {
            throw new RuntimeException("User is already enrolled in this class");
        }

        Enrollment enrollment = new Enrollment(userId, classId, role);
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Get enrollment by ID
     */
    public Optional<Enrollment> getEnrollmentById(String id) {
        return enrollmentRepository.findById(id);
    }

    /**
     * Get enrollment for a specific user in a class
     */
    public Optional<Enrollment> getEnrollment(String userId, String classId) {
        return enrollmentRepository.findByUserIdAndClassId(userId, classId);
    }

    /**
     * Get all enrollments for a user
     */
    public List<Enrollment> getEnrollmentsByUser(String userId) {
        return enrollmentRepository.findByUserId(userId);
    }

    /**
     * Get active enrollments for a user
     */
    public List<Enrollment> getActiveEnrollmentsByUser(String userId) {
        return enrollmentRepository.findByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE);
    }

    /**
     * Get all enrollments for a class
     */
    public List<Enrollment> getEnrollmentsByClass(String classId) {
        return enrollmentRepository.findByClassId(classId);
    }

    /**
     * Get students enrolled in a class
     */
    public List<Enrollment> getStudents(String classId) {
        return enrollmentRepository.findByClassIdAndRole(classId, EnrollmentRole.STUDENT);
    }

    /**
     * Get active students in a class
     */
    public List<Enrollment> getActiveStudents(String classId) {
        return enrollmentRepository.findByClassIdAndRoleAndStatus(
                classId, EnrollmentRole.STUDENT, EnrollmentStatus.ACTIVE);
    }

    /**
     * Get TAs for a class
     */
    public List<Enrollment> getTAs(String classId) {
        return enrollmentRepository.findByClassIdAndRole(classId, EnrollmentRole.TA);
    }

    /**
     * Get active TAs for a class
     */
    public List<Enrollment> getActiveTAs(String classId) {
        return enrollmentRepository.findByClassIdAndRoleAndStatus(
                classId, EnrollmentRole.TA, EnrollmentStatus.ACTIVE);
    }

    /**
     * Get all staff (teachers and TAs) for a class
     */
    public List<Enrollment> getStaff(String classId) {
        List<Enrollment> tas = getTAs(classId);
        List<Enrollment> teachers = enrollmentRepository.findByClassIdAndRole(classId, EnrollmentRole.TEACHER);
        tas.addAll(teachers);
        return tas;
    }

    /**
     * Upgrade a student to TA
     */
    public Enrollment upgradeToTA(String enrollmentId, String performedBy) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(enrollmentId);
        if (enrollmentOpt.isEmpty()) {
            throw new RuntimeException("Enrollment not found with id: " + enrollmentId);
        }

        Enrollment enrollment = enrollmentOpt.get();
        enrollment.upgradeToTA(performedBy);
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Change enrollment role
     */
    public Enrollment changeRole(String enrollmentId, EnrollmentRole newRole, String performedBy) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(enrollmentId);
        if (enrollmentOpt.isEmpty()) {
            throw new RuntimeException("Enrollment not found with id: " + enrollmentId);
        }

        Enrollment enrollment = enrollmentOpt.get();
        enrollment.setRoleAndUpdate(newRole, performedBy);
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Drop an enrollment
     */
    public Enrollment dropEnrollment(String enrollmentId) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(enrollmentId);
        if (enrollmentOpt.isEmpty()) {
            throw new RuntimeException("Enrollment not found with id: " + enrollmentId);
        }

        Enrollment enrollment = enrollmentOpt.get();
        enrollment.drop();
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Complete an enrollment (end of term)
     */
    public Enrollment completeEnrollment(String enrollmentId) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(enrollmentId);
        if (enrollmentOpt.isEmpty()) {
            throw new RuntimeException("Enrollment not found with id: " + enrollmentId);
        }

        Enrollment enrollment = enrollmentOpt.get();
        enrollment.complete();
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Delete an enrollment
     */
    public void deleteEnrollment(String id) {
        enrollmentRepository.deleteById(id);
    }

    /**
     * Check if a user is enrolled in a class
     */
    public boolean isEnrolled(String userId, String classId) {
        return enrollmentRepository.existsByUserIdAndClassId(userId, classId);
    }

    /**
     * Check if a user is an active student in a class
     */
    public boolean isActiveStudent(String userId, String classId) {
        Optional<Enrollment> enrollment = enrollmentRepository.findByUserIdAndClassId(userId, classId);
        return enrollment.map(e -> e.isStudent() && e.isActive()).orElse(false);
    }

    /**
     * Check if a user is an active TA in a class
     */
    public boolean isActiveTA(String userId, String classId) {
        Optional<Enrollment> enrollment = enrollmentRepository.findByUserIdAndClassId(userId, classId);
        return enrollment.map(e -> e.isTA() && e.isActive()).orElse(false);
    }

    /**
     * Check if a user is active staff (TA or teacher) in a class
     */
    public boolean isActiveStaff(String userId, String classId) {
        Optional<Enrollment> enrollment = enrollmentRepository.findByUserIdAndClassId(userId, classId);
        return enrollment.map(e -> e.isStaff() && e.isActive()).orElse(false);
    }

    /**
     * Count students in a class
     */
    public long countStudents(String classId) {
        return enrollmentRepository.countByClassIdAndRole(classId, EnrollmentRole.STUDENT);
    }

    /**
     * Count TAs in a class
     */
    public long countTAs(String classId) {
        return enrollmentRepository.countByClassIdAndRole(classId, EnrollmentRole.TA);
    }
}
