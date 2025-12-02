package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.LtiJwtValidator;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LtiLaunchControllerTest {

    private final LtiJwtValidator validator = Mockito.mock(LtiJwtValidator.class);
    private final StateNonceStore stateNonceStore = Mockito.mock(StateNonceStore.class);

    @Test
    void launch_validationEnabled_validatorMissing_returns500() {
        when(stateNonceStore.consumeNonce("state123")).thenReturn(Optional.of("nonce"));
        LtiLaunchController controller = new LtiLaunchController(null, stateNonceStore, true);

        var response = controller.launch("token", "state123", new MockHttpSession());

        assertEquals(500, response.getStatusCode().value());
        assertTrue(((Map<?, ?>) response.getBody()).get("error").toString().contains("validator"));
    }

    @Test
    void launch_validationEnabled_missingState_returns401() throws Exception {
        when(stateNonceStore.consumeNonce("expired")).thenReturn(Optional.empty());
        LtiLaunchController controller = new LtiLaunchController(validator, stateNonceStore, true);

        var response = controller.launch("token", "expired", new MockHttpSession());

        assertEquals(401, response.getStatusCode().value());
        assertEquals("invalid_or_expired_state", ((Map<?, ?>) response.getBody()).get("error"));
        verifyNoInteractions(validator);
    }

    @Test
    void launch_validationEnabled_success_setsSessionAndReturnsClaims() throws Exception {
        when(stateNonceStore.consumeNonce("valid")).thenReturn(Optional.of("nonce"));
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("iss")
                .audience("aud")
                .subject("sub123")
                .claim("name", "Student User")
                .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id", "deploy1")
                .claim("https://purl.imsglobal.org/spec/lti/claim/context", Map.of("id", "ctx"))
                .claim("https://purl.imsglobal.org/spec/lti/claim/roles",
                        List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor"))
                .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", "LtiResourceLinkRequest")
                .build();
        when(validator.validate(anyString(), anyString())).thenReturn(claims);
        HttpSession session = new MockHttpSession();
        LtiLaunchController controller = new LtiLaunchController(validator, stateNonceStore, true);

        var response = controller.launch("token-123", "valid", session);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("sub123", body.get("sub"));
        assertTrue(((Set<?>) session.getAttribute("ltiRoles")).contains("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor"));
        assertEquals("deploy1", session.getAttribute("ltiDeploymentId"));
    }

    @Test
    void launch_validationDisabled_mockModeReturnsOk() {
        when(stateNonceStore.consumeNonce("any")).thenReturn(Optional.of("nonce"));
        LtiLaunchController controller = new LtiLaunchController(null, stateNonceStore, false);

        var response = controller.launch("tok", "any", new MockHttpSession());

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("ok (validation disabled)", body.get("status"));
        assertTrue(((Set<?>) body.get("roles")).iterator().next().toString().contains("Instructor"));
    }

    @Test
    void launch_validationEnabled_invalidJwt_returns401() throws Exception {
        when(stateNonceStore.consumeNonce("state")).thenReturn(Optional.of("nonce"));
        when(validator.validate(anyString(), anyString())).thenThrow(new RuntimeException("bad jwt"));
        LtiLaunchController controller = new LtiLaunchController(validator, stateNonceStore, true);

        var response = controller.launch("bad-token", "state", new MockHttpSession());

        assertEquals(401, response.getStatusCode().value());
        assertEquals("invalid_id_token", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void launch_validationEnabled_rolesClaimMissing_defaultsToEmptySet() throws Exception {
        when(stateNonceStore.consumeNonce("nostate")).thenReturn(Optional.of("nonce"));
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("iss")
                .audience("aud")
                .subject("sub123")
                .build();
        when(validator.validate(anyString(), anyString())).thenReturn(claims);
        MockHttpSession session = new MockHttpSession();
        LtiLaunchController controller = new LtiLaunchController(validator, stateNonceStore, true);

        var response = controller.launch("token", "nostate", session);

        assertEquals(200, response.getStatusCode().value());
        Set<?> roles = (Set<?>) session.getAttribute("ltiRoles");
        assertTrue(roles.isEmpty());
    }
}
