package com.example.lab_signoff_backend.security;

/**
 * Constants for LTI role URIs as defined by IMS Global specifications.
 *
 * This class contains the standard role identifiers used in LTI 1.3
 * for identifying user roles within the learning platform.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
public final class LtiRoles {
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private LtiRoles() {}

    /**
     * LTI role URI for Instructor.
     */
    public static final String INSTRUCTOR =
            "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor";

    /**
     * LTI role URI for Teaching Assistant.
     */
    public static final String TA =
            "http://purl.imsglobal.org/vocab/lis/v2/membership#TeachingAssistant";

    /**
     * LTI role URI for Learner (student).
     */
    public static final String LEARNER =
            "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner";
}
