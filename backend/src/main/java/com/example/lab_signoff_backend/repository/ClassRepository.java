package com.example.lab_signoff_backend.repository;

import com.example.lab_signoff_backend.model.Class;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Class entity
 * Provides CRUD operations and custom queries for Class/Course management
 */
@Repository
public interface ClassRepository extends MongoRepository<Class, String> {

    /**
     * Find all classes taught by a specific instructor
     *
     * @param instructorId The instructor's user ID
     * @return List of classes taught by the instructor
     */
    List<Class> findByInstructorId(String instructorId);

    /**
     * Find classes by term and archived status
     *
     * @param term     The academic term (e.g., "Fall 2025")
     * @param archived Whether the class is archived
     * @return List of classes matching the criteria
     */
    List<Class> findByTermAndArchived(String term, Boolean archived);

    /**
     * Find a specific class by course code, term, and section
     *
     * @param courseCode The course code (e.g., "CSCI-475")
     * @param term       The academic term
     * @param section    The section number
     * @return Optional containing the class if found
     */
    Optional<Class> findByCourseCodeAndTermAndSection(String courseCode, String term, String section);

    /**
     * Find all classes that have a specific student in their roster
     *
     * @param studentId The student's user ID
     * @return List of classes the student is enrolled in
     */
    List<Class> findByRosterContaining(String studentId);

    /**
     * Find all classes for a specific term
     *
     * @param term The academic term
     * @return List of classes in that term
     */
    List<Class> findByTerm(String term);

    /**
     * Find all non-archived classes
     *
     * @return List of active classes
     */
    List<Class> findByArchivedFalse();

    /**
     * Check if a class with the given course code, term, and section exists
     *
     * @param courseCode The course code
     * @param term       The academic term
     * @param section    The section number
     * @return true if exists, false otherwise
     */
    boolean existsByCourseCodeAndTermAndSection(String courseCode, String term, String section);
}
