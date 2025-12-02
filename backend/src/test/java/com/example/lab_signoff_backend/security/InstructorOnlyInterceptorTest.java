package com.example.lab_signoff_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InstructorOnlyInterceptorTest {

    @Test
    void allowsInstructorRole() throws Exception {
        var interceptor = new InstructorOnlyInterceptor(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute("ltiRoles")).thenReturn(Set.of(LtiRoles.INSTRUCTOR));

        assertTrue(interceptor.preHandle(req, res, new Object()));
    }

    @Test
    void forbidsMissingSession() throws Exception {
        var interceptor = new InstructorOnlyInterceptor(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        when(req.getSession(false)).thenReturn(null);

        assertFalse(interceptor.preHandle(req, res, new Object()));
        verify(res).sendError(HttpServletResponse.SC_UNAUTHORIZED, "No LTI session");
    }

    @Test
    void forbidsWhenRolesMissing() throws Exception {
        var interceptor = new InstructorOnlyInterceptor(false);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute("ltiRoles")).thenReturn(null);

        assertFalse(interceptor.preHandle(req, res, new Object()));
        verify(res).sendError(HttpServletResponse.SC_FORBIDDEN, "Missing LTI roles");
    }

    @Test
    void forbidsWhenNotInstructorOrTa() throws Exception {
        var interceptor = new InstructorOnlyInterceptor(true);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute("ltiRoles")).thenReturn(Set.of("Student"));

        assertFalse(interceptor.preHandle(req, res, new Object()));
        verify(res).sendError(HttpServletResponse.SC_FORBIDDEN, "Instructor (or TA) role required");
    }
}
