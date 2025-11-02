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

            // Extract firstName and lastName from Auth0 (given_name and family_name)
            String firstName = (String) auth0User.get("given_name");
            String lastName = (String) auth0User.get("family_name");

            // Fallback: if firstName/lastName not provided, try to parse from name
            if (firstName == null && name != null && !name.isEmpty()) {
                String[] nameParts = name.split(" ", 2);
                firstName = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : "";
            }

            // Extract roles from custom claim
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) auth0User.get("https://lab-signoff-app/roles");
            if (roles == null || roles.isEmpty()) {
                roles = Arrays.asList("Teacher");
            }

            // Check if user exists
            Optional<User> existingUser = userService.findByAuth0Id(auth0Id);

            User user;
            if (existingUser.isPresent()) {
                // Update existing user
                user = existingUser.get();
                user.setEmail(email);
                user.setName(name);

                // Only update firstName/lastName if they're currently null/empty in the database
                // This preserves user-entered data from the profile completion modal
                if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
                    user.setFirstName(firstName);
                }
                if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
                    user.setLastName(lastName);
                }

                // Only update roles if they're currently null/empty in the database
                // This preserves manually assigned roles (e.g., Admin) from being overwritten
                if (user.getRoles() == null || user.getRoles().isEmpty()) {
                    System.out.println("⚠️  No roles in database - setting default roles: " + roles);
                    user.setRoles(roles);
                } else {
                    System.out.println("✓ Preserving existing roles: " + user.getRoles() + " (not overwriting with: " + roles + ")");
                }

                user.setPicture(picture);
                user.updateLastLogin();
            } else {
                // Create new user
                user = new User(auth0Id, email, name, picture, roles);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setCreatedAt(Instant.now());
                user.setLastLogin(Instant.now());
            }

            // Save to MongoDB
            user = userService.syncUserFromAuth0Data(user);

            // Log the user data being returned
            System.out.println("✅ Syncing user to MongoDB:");
            System.out.println("   - Email: " + user.getEmail());
            System.out.println("   - FirstName: " + user.getFirstName());
            System.out.println("   - LastName: " + user.getLastName());
            System.out.println("   - PrimaryRole: " + user.getPrimaryRole());
            System.out.println("   - Roles: " + user.getRoles());

            // Return user data
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
                response.put("firstName", user.getFirstName());
                response.put("lastName", user.getLastName());
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
