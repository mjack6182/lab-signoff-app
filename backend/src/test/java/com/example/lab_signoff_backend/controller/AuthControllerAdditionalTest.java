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
class AuthControllerAdditionalTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void syncUser_preservesExistingNameWhenFirstLastPresent() throws Exception {
        User existing = new User("auth0|keep", "old@example.com", "Old Name", null, List.of("Teacher"));
        existing.setId("u-keep");
        existing.setFirstName("Existing");
        existing.setLastName("User");
        when(userService.findByAuth0Id("auth0|keep")).thenReturn(Optional.of(existing));
        when(userService.syncUserFromAuth0Data(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/auth/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sub\":\"auth0|keep\",\"email\":\"new@example.com\",\"name\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.firstName").value("Existing"))
                .andExpect(jsonPath("$.user.lastName").value("User"));
    }
}
