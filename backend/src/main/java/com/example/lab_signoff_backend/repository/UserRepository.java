package com.example.lab_signoff_backend.repository;

import com.example.lab_signoff_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Provides CRUD operations and custom query methods for users.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Find a user by their Auth0 ID
     *
     * @param auth0Id The Auth0 user ID (subject claim)
     * @return Optional containing the user if found
     */
    Optional<User> findByAuth0Id(String auth0Id);

    /**
     * Find a user by their email address
     *
     * @param email The user's email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all users with a specific role
     *
     * @param role The role to search for (e.g., "Teacher", "TA", "Student")
     * @return List of users with the specified role
     */
    List<User> findByRolesContaining(String role);

    /**
     * Find all users with a specific primary role
     *
     * @param primaryRole The primary role to search for
     * @return List of users with the specified primary role
     */
    List<User> findByPrimaryRole(String primaryRole);

    /**
     * Check if a user exists with the given Auth0 ID
     *
     * @param auth0Id The Auth0 user ID
     * @return true if user exists, false otherwise
     */
    boolean existsByAuth0Id(String auth0Id);

    /**
     * Check if a user exists with the given email
     *
     * @param email The user's email
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
}
