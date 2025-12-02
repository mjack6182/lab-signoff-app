package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.User;
import com.example.lab_signoff_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void findByIds_nullOrEmptyReturnsEmptyMap() {
        assertTrue(userService.findByIds(null).isEmpty());
        assertTrue(userService.findByIds(List.of()).isEmpty());
    }

    @Test
    void findByIds_returnsMappedUsers() {
        User u = new User("auth0|1", "a@example.com", "A", null, List.of("Teacher"));
        u.setId("u1");
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(u));

        Map<String, User> map = userService.findByIds(List.of("u1"));
        assertEquals("a@example.com", map.get("u1").getEmail());
    }

    @Test
    void updateUserRoles_updatesWhenPresent() {
        User u = new User("auth0|2", "b@example.com", "B", null, List.of("Student"));
        when(userRepository.findByAuth0Id("auth0|2")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> updated = userService.updateUserRoles("auth0|2", List.of("TA"));
        assertTrue(updated.isPresent());
        assertEquals("TA", updated.get().getRoles().getFirst());
    }

    @Test
    void updateUserRoles_missingReturnsEmpty() {
        when(userRepository.findByAuth0Id("missing")).thenReturn(Optional.empty());
        assertTrue(userService.updateUserRoles("missing", List.of("Teacher")).isEmpty());
    }

    @Test
    void deleteByAuth0Id_deletesIfPresent() {
        User u = new User("auth0|3", "c@example.com", "C", null, List.of("Teacher"));
        when(userRepository.findByAuth0Id("auth0|3")).thenReturn(Optional.of(u));

        userService.deleteByAuth0Id("auth0|3");
        verify(userRepository).delete(u);
    }

    @Test
    void deleteByAuth0Id_noopWhenMissing() {
        when(userRepository.findByAuth0Id("missing")).thenReturn(Optional.empty());
        userService.deleteByAuth0Id("missing");
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void findByAuth0Id_presentReturnsUser() {
        User u = new User("auth0|present", "p@example.com", "P", null, List.of("Teacher"));
        when(userRepository.findByAuth0Id("auth0|present")).thenReturn(Optional.of(u));

        Optional<User> result = userService.findByAuth0Id("auth0|present");
        assertTrue(result.isPresent());
    }

    @Test
    void findByAuth0Id_missingReturnsEmpty() {
        when(userRepository.findByAuth0Id("none")).thenReturn(Optional.empty());
        assertTrue(userService.findByAuth0Id("none").isEmpty());
    }

    @Test
    void existsByAuth0Id_delegatesRepo() {
        when(userRepository.existsByAuth0Id("auth0|exists")).thenReturn(true);
        assertTrue(userService.existsByAuth0Id("auth0|exists"));
    }

    @Test
    void roleHelpers_delegateToRepository() {
        when(userRepository.findByRolesContaining("Teacher")).thenReturn(new java.util.ArrayList<>(List.of(new User())));
        when(userRepository.findByRolesContaining("TA")).thenReturn(new java.util.ArrayList<>(List.of(new User())));
        when(userRepository.findByRolesContaining("Student")).thenReturn(new java.util.ArrayList<>(List.of(new User())));

        assertEquals(1, userService.getTeachers().size());
        assertEquals(1, userService.getTAs().size());
        assertEquals(1, userService.getStudents().size());
        assertEquals(2, userService.getStaff().size());
    }

    @Test
    void syncUserFromAuth0Data_savesUser() {
        User u = new User("auth0|10", "d@example.com", "D", null, List.of("Teacher"));
        when(userRepository.save(u)).thenReturn(u);
        User saved = userService.syncUserFromAuth0Data(u);
        assertEquals("auth0|10", saved.getAuth0Id());
    }
}
