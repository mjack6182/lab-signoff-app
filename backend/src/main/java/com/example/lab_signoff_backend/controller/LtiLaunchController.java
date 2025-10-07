package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.LtiJwtValidator;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling LTI 1.3 launch requests from Canvas.
 *
 * This controller validates incoming LTI launch requests, verifies JWT tokens,
 * extracts user roles and context information, and establishes authenticated sessions.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@RestController
@RequestMapping("/lti")
public class LtiLaunchController {
    private final LtiJwtValidator validator;
    private final StateNonceStore stateNonceStore;

    /**
     * Constructor for LtiLaunchController.
     *
     * @param validator       The JWT validator for verifying LTI tokens
     * @param stateNonceStore Store for managing state-nonce pairs
     */
    public LtiLaunchController(LtiJwtValidator validator, StateNonceStore stateNonceStore) {
        this.validator = validator;
        this.stateNonceStore = stateNonceStore;
    }

    /**
     * Handles LTI launch requests from Canvas.
     *
     * Validates the ID token, verifies state and nonce, extracts user roles
     * and context information, and stores them in the HTTP session.
     *
     * @param idToken The JWT ID token from Canvas
     * @param state   The state parameter for CSRF protection
     * @param session The HTTP session for storing user information
     * @return ResponseEntity containing launch information or error details
     */
    @PostMapping(value = "/launch", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> launch(@RequestParam("id_token") String idToken,
                                    @RequestParam("state") String state,
                                    HttpSession session) {
        try {
            // In your real flow, you issued (state, nonce) at /lti/login and stored it
            Optional<String> expectedNonce = stateNonceStore.consumeNonce(state);
            if (expectedNonce.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid_or_expired_state"));
            }


            JWTClaimsSet claims = validator.validate(idToken, expectedNonce.get());


            @SuppressWarnings("unchecked")
            java.util.List<String> rolesList = (java.util.List<String>)
                    claims.getClaim("https://purl.imsglobal.org/spec/lti/claim/roles");
            if (rolesList == null) rolesList = java.util.List.of();
            java.util.Set<String> roles = new java.util.HashSet<>(rolesList);
            session.setAttribute("ltiRoles", roles);

            // (Optional) store other helpful bits for later requests:
            session.setAttribute("ltiUserSub", claims.getSubject());
            session.setAttribute("ltiDeploymentId",
                    claims.getStringClaim("https://purl.imsglobal.org/spec/lti/claim/deployment_id"));
            session.setAttribute("ltiContext",
                    claims.getClaim("https://purl.imsglobal.org/spec/lti/claim/context"));



            // Minimal response (you'll likely redirect into your UI here)
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("iss", claims.getIssuer());
            out.put("aud", claims.getAudience());
            out.put("sub", claims.getSubject());
            out.put("name", claims.getStringClaim("name"));
            out.put("roles", claims.getClaim("https://purl.imsglobal.org/spec/lti/claim/roles"));
            out.put("context", claims.getClaim("https://purl.imsglobal.org/spec/lti/claim/context"));
            out.put("deployment_id", claims.getClaim("https://purl.imsglobal.org/spec/lti/claim/deployment_id"));
            out.put("message_type", claims.getClaim("https://purl.imsglobal.org/spec/lti/claim/message_type"));

            return ResponseEntity.ok(out);



        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_id_token", "details", e.getMessage()));
        }
    }
}
