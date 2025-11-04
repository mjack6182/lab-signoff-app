package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.User;
import com.example.lab_signoff_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling user profile operations
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Update user profile (firstName and lastName)
     * Request body should contain: { auth0Id, firstName, lastName }
     */
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, String> profileData) {
        try {
            String auth0Id = profileData.get("auth0Id");
            String firstName = profileData.get("firstName");
            String lastName = profileData.get("lastName");

            // Validate input
            if (auth0Id == null || auth0Id.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "auth0Id is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (firstName == null || firstName.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "firstName is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (lastName == null || lastName.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "lastName is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Find user
            Optional<User> userOpt = userService.findByAuth0Id(auth0Id);
            if (!userOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "User not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Update user
            User user = userOpt.get();
            user.setFirstName(firstName.trim());
            user.setLastName(lastName.trim());

            // Update full name as well for backward compatibility
            user.setName(firstName.trim() + " " + lastName.trim());

            // Save to database
            user = userService.syncUserFromAuth0Data(user);

            // Return updated user data
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("auth0Id", user.getAuth0Id());
            userData.put("email", user.getEmail());
            userData.put("name", user.getName());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());
            userData.put("picture", user.getPicture());
            userData.put("roles", user.getRoles());
            userData.put("primaryRole", user.getPrimaryRole());

            response.put("user", userData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Delete user account
     * Request body should contain: { auth0Id }
     */
    @DeleteMapping("/account")
    public ResponseEntity<Map<String, Object>> deleteAccount(@RequestBody Map<String, String> requestData) {
        try {
            String auth0Id = requestData.get("auth0Id");

            // Validate input
            if (auth0Id == null || auth0Id.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "auth0Id is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Check if user exists
            Optional<User> userOpt = userService.findByAuth0Id(auth0Id);
            if (!userOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "User not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Delete user from database
            userService.deleteByAuth0Id(auth0Id);

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
