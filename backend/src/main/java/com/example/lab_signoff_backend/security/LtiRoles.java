package com.example.lab_signoff_backend.security;

public final class LtiRoles {
    private LtiRoles() {}

    public static final String INSTRUCTOR =
            "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor";

    public static final String TA =
            "http://purl.imsglobal.org/vocab/lis/v2/membership#TeachingAssistant";

    public static final String LEARNER =
            "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner";
}
