package com.example.lab_signoff_backend.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserModelTest {

    @Test
    void constructorSetsDefaultsAndPrimaryRole() {
        User user = new User("auth0|1", "a@example.com", "Name", null, List.of("TA", "Student"));
        assertEquals("TA", user.getPrimaryRole());
        assertTrue(user.isTA());
        assertTrue(user.hasRole("Student"));
    }

    @Test
    void setRoles_updatesPrimaryRole() {
        User user = new User();
        user.setRoles(List.of("Student"));
        assertEquals("Student", user.getPrimaryRole());
        assertTrue(user.isStudent());
        user.setRoles(null);
        assertEquals("Teacher", user.getPrimaryRole());
        assertFalse(user.hasRole("Student"));
    }

    @Test
    void noRolesDefaultsTeacher() {
        User user = new User("auth0|2", "b@example.com", "Name", null, null);
        assertEquals("Teacher", user.getPrimaryRole());
        assertFalse(user.isTA());
    }

    @Test
    void hasRole_returnsFalseWhenRolesNull() {
        User user = new User();
        assertFalse(user.hasRole("Teacher"));
    }

    @Test
    void setRoles_emptyListDefaultsPrimaryTeacher() {
        User user = new User();
        user.setRoles(List.of());
        assertEquals("Teacher", user.getPrimaryRole());
    }

    @Test
    void isTeacherAndIsTAReflectRoles() {
        User user = new User();
        user.setRoles(List.of("Teacher", "TA"));
        assertTrue(user.isTeacher());
        assertTrue(user.hasRole("TA"));
    }

    @Test
    void setRoles_singleTAUpdatesPrimary() {
        User user = new User();
        user.setRoles(List.of("TA"));
        assertEquals("TA", user.getPrimaryRole());
        assertTrue(user.isTA());
    }

    @Test
    void staffAndAdminChecksReflectRoles() {
        User user = new User();
        user.setRoles(List.of("Admin", "Student"));
        assertTrue(user.isAdmin());
        assertTrue(user.isStaffOrAdmin());
        assertFalse(user.isStaff()); // no Teacher/TA
    }

    @Test
    void staffChecksReturnFalseWhenNoRoles() {
        User user = new User();
        assertFalse(user.isStaff());
        assertFalse(user.isStaffOrAdmin());
    }

    @Test
    void updateLastLoginRefreshesTimestamp() throws InterruptedException {
        User user = new User("auth0|3", "c@example.com", "Name", null, List.of("Teacher"));
        var initial = user.getLastLogin();
        Thread.sleep(2);
        user.updateLastLogin();
        assertTrue(user.getLastLogin().isAfter(initial));
    }
}
