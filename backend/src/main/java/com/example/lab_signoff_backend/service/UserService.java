package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.User;
import com.example.lab_signoff_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing user operations.
 * Handles user synchronization from Auth0 to MongoDB and user-related business logic.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Find a user by their Auth0 ID
     *
     * @param auth0Id The Auth0 user ID
     * @return Optional containing the user if found
     */
    public Optional<User> findByAuth0Id(String auth0Id) {
        System.out.println("📊 UserService: Querying MongoDB for user with auth0Id: " + auth0Id);
        Optional<User> result = userRepository.findByAuth0Id(auth0Id);
        System.out.println("📊 UserService: Query result - User " + (result.isPresent() ? "FOUND" : "NOT FOUND"));
        if (result.isPresent()) {
            User user = result.get();
            System.out.println("   - MongoDB ID: " + user.getId());
            System.out.println("   - Email: " + user.getEmail());
            System.out.println("   - Roles: " + user.getRoles());
        }
        return result;
    }

    /**
     * Find a user by their email
     *
     * @param email The user's email
     * @return Optional containing the user if found
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Get all users
     *
     * @return List of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get all users with a specific role
     *
     * @param role The role to filter by
     * @return List of users with the specified role
     */
    public List<User> getUsersByRole(String role) {
        return userRepository.findByRolesContaining(role);
    }

    /**
     * Get all teachers
     *
     * @return List of users with Teacher role
     */
    public List<User> getTeachers() {
        return getUsersByRole("Teacher");
    }

    /**
     * Get all TAs
     *
     * @return List of users with TA role
     */
    public List<User> getTAs() {
        return getUsersByRole("TA");
    }

    /**
     * Get all students
     *
     * @return List of users with Student role
     */
    public List<User> getStudents() {
        return getUsersByRole("Student");
    }

    /**
     * Get all staff (Teachers and TAs)
     *
     * @return List of users with Teacher or TA role
     */
    public List<User> getStaff() {
        List<User> staff = getTeachers();
        staff.addAll(getTAs());
        return staff;
    }

    /**
     * Check if a user exists by Auth0 ID
     *
     * @param auth0Id The Auth0 user ID
     * @return true if user exists, false otherwise
     */
    public boolean existsByAuth0Id(String auth0Id) {
        return userRepository.existsByAuth0Id(auth0Id);
    }

    /**
     * Update a user's roles
     *
     * @param auth0Id The Auth0 user ID
     * @param roles   The new list of roles
     * @return Optional containing the updated user if found
     */
    public Optional<User> updateUserRoles(String auth0Id, List<String> roles) {
        Optional<User> userOpt = userRepository.findByAuth0Id(auth0Id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setRoles(roles);
            return Optional.of(userRepository.save(user));
        }
        return Optional.empty();
    }

    /**
     * Delete a user by Auth0 ID
     *
     * @param auth0Id The Auth0 user ID
     */
    public void deleteByAuth0Id(String auth0Id) {
        userRepository.findByAuth0Id(auth0Id).ifPresent(userRepository::delete);
    }

    /**
     * Sync user from Auth0 data (called from frontend)
     *
     * @param user The user object with Auth0 data
     * @return The saved User entity
     */
    public User syncUserFromAuth0Data(User user) {
        return userRepository.save(user);
    }
}
