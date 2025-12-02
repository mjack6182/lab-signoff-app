package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.User;
import com.example.lab_signoff_backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void syncUser_existingUser_preservesRolesAndParsesNameFallback() throws Exception {
        User existing = new User("auth0|existing", "old@example.com", "Existing User", null, List.of("Admin"));
        existing.setId("user-1");
        existing.setFirstName("");
        existing.setLastName("");
        when(userService.findByAuth0Id("auth0|existing")).thenReturn(Optional.of(existing));
        when(userService.syncUserFromAuth0Data(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sub":"auth0|existing",
                                  "email":"new@example.com",
                                  "name":"Pat Example"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value("user-1"))
                .andExpect(jsonPath("$.user.firstName").value("Pat"))
                .andExpect(jsonPath("$.user.lastName").value("Example"))
                .andExpect(jsonPath("$.user.roles[0]").value("Admin"));
    }

    @Test
    void getCurrentUser_found_returnsAuthenticatedUser() throws Exception {
        User user = new User("auth0|found", "f@example.com", "Found User", null, List.of("Teacher"));
        user.setId("u-found");
        user.setFirstName("Found");
        when(userService.findByAuth0Id("auth0|found")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sub\":\"auth0|found\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.id").value("u-found"))
                .andExpect(jsonPath("$.email").value("f@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("Teacher"));
    }

    @Test
    void syncUser_createsNewUser() throws Exception {
        User user = new User("auth0|1", "a@example.com", "Alice Example", null, List.of("Teacher"));
        user.setId("u1");
        when(userService.findByAuth0Id("auth0|1")).thenReturn(Optional.empty());
        when(userService.syncUserFromAuth0Data(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sub":"auth0|1",
                                  "email":"a@example.com",
                                  "name":"Alice Example",
                                  "given_name":"Alice",
                                  "family_name":"Example"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.id").value("u1"));
    }

    @Test
    void getCurrentUser_notFound_returnsUnauthenticated() throws Exception {
        when(userService.findByAuth0Id("auth0|missing")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sub\":\"auth0|missing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void syncUser_handlesException() throws Exception {
        when(userService.findByAuth0Id("auth0|err")).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sub\":\"auth0|err\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentUser_whenServiceThrows_returnsBadRequest() throws Exception {
        when(userService.findByAuth0Id("auth0|problem")).thenThrow(new RuntimeException("db down"));

        mockMvc.perform(post("/api/auth/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sub\":\"auth0|problem\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.error").value("db down"));
    }

    @Test
    void syncUser_defaultsRoleToTeacherWhenMissing() throws Exception {
        User user = new User("auth0|noRole", "nr@example.com", "No Role", null, List.of("Teacher"));
        user.setId("u-role");
        when(userService.findByAuth0Id("auth0|noRole")).thenReturn(Optional.empty());
        when(userService.syncUserFromAuth0Data(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sub\":\"auth0|noRole\",\"email\":\"nr@example.com\",\"name\":\"No Role\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles[0]").value("Teacher"));
    }

    @Test
    void syncUser_existingUserWithNoRoles_getsRolesFromPayload() throws Exception {
        User existing = new User("auth0|noroles", "old@example.com", "Old Name", null, List.of());
        existing.setId("u-no-role");
        when(userService.findByAuth0Id("auth0|noroles")).thenReturn(Optional.of(existing));
        when(userService.syncUserFromAuth0Data(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sub\":\"auth0|noroles\",\"email\":\"new@example.com\",\"name\":\"Old Name\",\"https://lab-signoff-app/roles\":[\"TA\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles[0]").value("TA"));
    }
}
