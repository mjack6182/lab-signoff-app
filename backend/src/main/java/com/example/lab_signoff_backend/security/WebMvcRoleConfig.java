package com.example.lab_signoff_backend.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcRoleConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Protect ALL teacher APIs (adjust the path pattern to your routes)
        registry.addInterceptor(new InstructorOnlyInterceptor(/*allowTAs=*/true))
                .addPathPatterns("/api/teacher/**", "/admin/**");
    }
}

