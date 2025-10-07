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

@RestController
@RequestMapping("/lti")
public class LtiLaunchController {
    private final LtiJwtValidator validator;

    public LtiLaunchController(LtiJwtValidator validator) {
        this.validator = validator;
    }

    @PostMapping(value = "/launch", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> launch(@RequestParam("id_token") String idToken,
                                    @RequestParam("state") String state,
                                    HttpSession session) {
        try {
            // In your real flow, you issued (state, nonce) at /lti/login and stored it
            Optional<String> expectedNonce = StateNonceStore.consumeNonce(state);
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



            // Minimal response (you’ll likely redirect into your UI here)
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
