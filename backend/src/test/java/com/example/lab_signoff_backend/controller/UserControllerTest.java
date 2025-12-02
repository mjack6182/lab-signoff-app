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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void updateProfile_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auth0Id\":\"\",\"firstName\":\"\",\"lastName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_missingAuth0Id_only() throws Exception {
        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auth0Id\":\"\",\"firstName\":\"First\",\"lastName\":\"Last\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_missingFirstName_only() throws Exception {
        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auth0Id\":\"auth0|1\",\"lastName\":\"Last\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_missingLastName_only() throws Exception {
        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auth0Id\":\"auth0|1\",\"firstName\":\"First\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_success_updatesUser() throws Exception {
        User user = new User("auth0|1", "a@example.com", "Alice Example", null, List.of("Teacher"));
        user.setId("u1");
        when(userService.findByAuth0Id("auth0|1")).thenReturn(Optional.of(user));
        when(userService.syncUserFromAuth0Data(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auth0Id\":\"auth0|1\",\"firstName\":\"Alice\",\"lastName\":\"Example\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.name").value("Alice Example"));
    }

    @Test
    void deleteAccount_missingAuth_returnsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/users/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auth0Id\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAccount_success_returnsOk() throws Exception {
        User user = new User("auth0|1", "a@example.com", "Alice Example", null, List.of("Teacher"));
        when(userService.findByAuth0Id("auth0|1")).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/api/users/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auth0Id\":\"auth0|1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteAccount_notFound_returnsBadRequest() throws Exception {
        when(userService.findByAuth0Id("missing")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/users/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auth0Id\":\"missing\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_userNotFound_returnsBadRequest() throws Exception {
        when(userService.findByAuth0Id("missing")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auth0Id\":\"missing\",\"firstName\":\"A\",\"lastName\":\"B\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_handlesExceptionReturnsBadRequest() throws Exception {
        when(userService.findByAuth0Id("err")).thenThrow(new RuntimeException("db down"));

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auth0Id\":\"err\",\"firstName\":\"A\",\"lastName\":\"B\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAccount_handlesExceptionReturnsBadRequest() throws Exception {
        when(userService.findByAuth0Id("err")).thenThrow(new RuntimeException("db down"));

        mockMvc.perform(delete("/api/users/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auth0Id\":\"err\"}"))
                .andExpect(status().isBadRequest());
    }
}
