package com.example.lab_signoff_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Interceptor that restricts access to instructor-only endpoints.
 *
 * This interceptor validates that the user has appropriate LTI roles
 * (Instructor or optionally Teaching Assistant) before allowing access
 * to protected endpoints.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
public class InstructorOnlyInterceptor implements HandlerInterceptor {

    private final boolean allowTAs;

    /**
     * Constructor for InstructorOnlyInterceptor.
     *
     * @param allowTAs Whether to allow Teaching Assistants access along with Instructors
     */
    public InstructorOnlyInterceptor(boolean allowTAs) {
        this.allowTAs = allowTAs;
    }

    /**
     * Validates user's LTI roles before allowing access to protected endpoints.
     *
     * @param req     The HTTP request
     * @param res     The HTTP response
     * @param handler The handler being invoked
     * @return true if the user has appropriate roles, false otherwise
     * @throws Exception if an error occurs during processing
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean preHandle(@org.springframework.lang.NonNull HttpServletRequest req,
                             @org.springframework.lang.NonNull HttpServletResponse res,
                             @org.springframework.lang.NonNull Object handler)
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