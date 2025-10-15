package com.example.lab_signoff_backend.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for role-based access control.
 *
 * This configuration registers interceptors to protect endpoints based on
 * LTI user roles, ensuring that only authorized users (instructors and TAs)
 * can access specific routes.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@Configuration
public class WebMvcRoleConfig implements WebMvcConfigurer {

    /**
     * Registers interceptors for role-based access control.
     *
     * Configures the InstructorOnlyInterceptor to protect teacher and admin
     * endpoints, allowing both instructors and teaching assistants access.
     *
     * @param registry The interceptor registry
     */
    @SuppressWarnings("null")
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Protect ALL teacher APIs (adjust the path pattern to your routes)
        registry.addInterceptor(new InstructorOnlyInterceptor(/*allowTAs=*/true))
                .addPathPatterns("/api/teacher/**", "/admin/**");
    }
}

