package com.example.lab_signoff_backend.repository;

import com.example.lab_signoff_backend.model.Enrollment;
import com.example.lab_signoff_backend.model.enums.EnrollmentRole;
import com.example.lab_signoff_backend.model.enums.EnrollmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Enrollment entity
 * Manages user enrollments in classes with role and status tracking
 */
@Repository
public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {

    /**
     * Find all enrollments for a specific user
     *
     * @param userId The user's ID
     * @return List of enrollments for the user
     */
    List<Enrollment> findByUserId(String userId);

    /**
     * Find all enrollments for a specific class
     *
     * @param classId The class ID
     * @return List of enrollments in the class
     */
    List<Enrollment> findByClassId(String classId);

    /**
     * Find enrollments in a class filtered by role
     *
     * @param classId The class ID
     * @param role    The enrollment role (STUDENT, TA, TEACHER)
     * @return List of enrollments matching the role
     */
    List<Enrollment> findByClassIdAndRole(String classId, EnrollmentRole role);

    /**
     * Find enrollments in a class filtered by status
     *
     * @param classId The class ID
     * @param status  The enrollment status (ACTIVE, DROPPED, COMPLETED)
     * @return List of enrollments matching the status
     */
    List<Enrollment> findByClassIdAndStatus(String classId, EnrollmentStatus status);

    /**
     * Find active enrollments in a class with a specific role
     *
     * @param classId The class ID
     * @param role    The enrollment role
     * @param status  The enrollment status
     * @return List of matching enrollments
     */
    List<Enrollment> findByClassIdAndRoleAndStatus(String classId, EnrollmentRole role, EnrollmentStatus status);

    /**
     * Find a specific enrollment for a user in a class
     *
     * @param userId  The user's ID
     * @param classId The class ID
     * @return Optional containing the enrollment if found
     */
    Optional<Enrollment> findByUserIdAndClassId(String userId, String classId);

    /**
     * Check if a user is enrolled in a specific class
     *
     * @param userId  The user's ID
     * @param classId The class ID
     * @return true if enrollment exists, false otherwise
     */
    boolean existsByUserIdAndClassId(String userId, String classId);

    /**
     * Find all active enrollments for a user
     *
     * @param userId The user's ID
     * @param status The enrollment status
     * @return List of active enrollments
     */
    List<Enrollment> findByUserIdAndStatus(String userId, EnrollmentStatus status);

    /**
     * Count enrollments in a class by role
     *
     * @param classId The class ID
     * @param role    The enrollment role
     * @return Count of enrollments with that role
     */
    long countByClassIdAndRole(String classId, EnrollmentRole role);
}
