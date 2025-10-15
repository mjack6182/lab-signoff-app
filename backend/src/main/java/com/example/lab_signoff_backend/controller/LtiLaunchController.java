package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.LtiJwtValidator;
import com.example.lab_signoff_backend.security.StateNonceStore;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.lang.Nullable;          // <-- use Spring’s @Nullable
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;

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
    private final @Nullable LtiJwtValidator validator;   // may be null when disabled
    private final boolean validationEnabled;

    public LtiLaunchController(@Nullable LtiJwtValidator validator,
                               @Value("${lti.validation-enabled:false}") boolean validationEnabled) {
        this.validator = validator;
        this.validationEnabled = validationEnabled;
    }

    @PostMapping(value = "/launch",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> launch(@RequestParam("id_token") String idToken,
                                    @RequestParam("state") String state,
                                    HttpSession session) {
        try {
            // Always consume state once (mock or real) to keep flow consistent
            Optional<String> expectedNonce = StateNonceStore.consumeNonce(state);
            if (validationEnabled) {
                // REAL PATH: validator must exist and nonce must be present
                if (validator == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "validator_not_available"));
                }
                if (expectedNonce.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "invalid_or_expired_state"));
                }

                @SuppressWarnings("null")
                JWTClaimsSet claims = validator.validate(idToken, expectedNonce.get());

                @SuppressWarnings("unchecked")
                List<String> rolesList = (List<String>) claims.getClaim(
                        "https://purl.imsglobal.org/spec/lti/claim/roles");
                if (rolesList == null) rolesList = List.of();
                Set<String> roles = new HashSet<>(rolesList);
                session.setAttribute("ltiRoles", roles);
                session.setAttribute("ltiUserSub", claims.getSubject());
                session.setAttribute("ltiDeploymentId",
                        claims.getStringClaim("https://purl.imsglobal.org/spec/lti/claim/deployment_id"));
                session.setAttribute("ltiContext",
                        claims.getClaim("https://purl.imsglobal.org/spec/lti/claim/context"));

                Map<String, Object> out = new LinkedHashMap<>();
                out.put("iss", claims.getIssuer());
                out.put("aud", claims.getAudience());
                out.put("sub", claims.getSubject());
                out.put("name", claims.getStringClaim("name"));
                out.put("roles", roles);
                out.put("context", claims.getClaim("https://purl.imsglobal.org/spec/lti/claim/context"));
                out.put("deployment_id", claims.getClaim("https://purl.imsglobal.org/spec/lti/claim/deployment_id"));
                out.put("message_type", claims.getClaim("https://purl.imsglobal.org/spec/lti/claim/message_type"));
                return ResponseEntity.ok(out);
            } else {
                // MOCK/DEV PATH: validation disabled — don’t require a real JWT
                // You can still allow your teacher console by seeding Instructor for the session:
                Set<String> roles = Set.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor");
                session.setAttribute("ltiRoles", roles);

                Map<String, Object> out = new LinkedHashMap<>();
                out.put("endpoint", "/lti/launch");
                out.put("status", "ok (validation disabled)");
                out.put("id_token", idToken);
                out.put("state", state);
                out.put("roles", roles);
                return ResponseEntity.ok(out);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_id_token", "details", e.getMessage()));
        }
    }
}
