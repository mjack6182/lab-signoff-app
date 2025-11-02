package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.User;
import com.example.lab_signoff_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Controller for handling Auth0 user synchronization
 * Since the frontend handles Auth0 authentication, this controller
 * receives user data from the frontend and syncs it to MongoDB
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Sync user from Auth0 to MongoDB
     * Called by frontend after successful Auth0 authentication
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncUser(@RequestBody Map<String, Object> auth0User) {
        try {
            // Extract user data from Auth0 response
            String auth0Id = (String) auth0User.get("sub");
            String email = (String) auth0User.get("email");
            String name = (String) auth0User.get("name");
            String picture = (String) auth0User.get("picture");

            // Extract roles from custom claim
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) auth0User.get("https://lab-signoff-app/roles");
            if (roles == null || roles.isEmpty()) {
                roles = Arrays.asList("Student");
            }

            // Check if user exists
            Optional<User> existingUser = userService.findByAuth0Id(auth0Id);

            User user;
            if (existingUser.isPresent()) {
                // Update existing user
                user = existingUser.get();
                user.setEmail(email);
                user.setName(name);
                user.setPicture(picture);
                user.setRoles(roles);
                user.updateLastLogin();
            } else {
                // Create new user
                user = new User(auth0Id, email, name, picture, roles);
                user.setCreatedAt(Instant.now());
                user.setLastLogin(Instant.now());
            }

            // Save to MongoDB
            user = userService.syncUserFromAuth0Data(user);

            // Return user data
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", Map.of(
                "id", user.getId(),
                "auth0Id", user.getAuth0Id(),
                "email", user.getEmail(),
                "name", user.getName(),
                "picture", user.getPicture(),
                "roles", user.getRoles(),
                "primaryRole", user.getPrimaryRole()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get current user info (for testing without auth)
     */
    @PostMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestBody Map<String, Object> auth0User) {
        try {
            String auth0Id = (String) auth0User.get("sub");
            Optional<User> userOpt = userService.findByAuth0Id(auth0Id);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("auth0Id", user.getAuth0Id());
                response.put("email", user.getEmail());
                response.put("name", user.getName());
                response.put("picture", user.getPicture());
                response.put("roles", user.getRoles());
                response.put("primaryRole", user.getPrimaryRole());
                response.put("authenticated", true);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of("authenticated", false));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("authenticated", false, "error", e.getMessage()));
        }
    }
}
