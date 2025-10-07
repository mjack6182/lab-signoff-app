package com.example.lab_signoff_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

public class InstructorOnlyInterceptor implements HandlerInterceptor {

    private final boolean allowTAs;

    public InstructorOnlyInterceptor(boolean allowTAs) {
        this.allowTAs = allowTAs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler)
            throws Exception {
        HttpSession session = req.getSession(false);
        if (session == null) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No LTI session");
            return false;
        }

        Object rolesObj = session.getAttribute("ltiRoles");
        if (!(rolesObj instanceof Set)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing LTI roles");
            return false;
        }

        Set<String> roles = (Set<String>) rolesObj;

        boolean isInstructor = roles.contains(LtiRoles.INSTRUCTOR);
        boolean isTA = roles.contains(LtiRoles.TA);

        if (isInstructor || (allowTAs && isTA)) {
            return true; // allow
        }

        res.sendError(HttpServletResponse.SC_FORBIDDEN, "Instructor (or TA) role required");
        return false;
    }
}