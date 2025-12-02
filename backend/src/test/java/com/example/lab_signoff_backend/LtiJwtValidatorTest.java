package com.example.lab_signoff_backend;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LtiJwtValidatorTest {

    @Mock
    private ConfigurableJWTProcessor<SecurityContext> processor;

    @Test
    void validate_happyPathReturnsClaims() throws Exception {
        LtiJwtValidator validator = new LtiJwtValidator("issuer", "client", "http://example.com/jwks", 120);
        ReflectionTestUtils.setField(validator, "processor", processor);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("issuer")
                .audience("client")
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .issueTime(Date.from(Instant.now().minusSeconds(30)))
                .claim("nonce", "nonce-123")
                .claim("https://purl.imsglobal.org/spec/lti/claim/version", "1.3.0")
                .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", "LtiResourceLinkRequest")
                .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id", "dep-1")
                .build();
        when(processor.process(anyString(), isNull())).thenReturn(claims);

        JWTClaimsSet result = validator.validate("token", "nonce-123");
        assertEquals("issuer", result.getIssuer());
        assertEquals("client", result.getAudience().getFirst());
    }

    @Test
    void validate_nonceMismatchThrows() throws Exception {
        LtiJwtValidator validator = new LtiJwtValidator("issuer", "client", "http://example.com/jwks", 60);
        ReflectionTestUtils.setField(validator, "processor", processor);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("issuer")
                .audience("client")
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .issueTime(Date.from(Instant.now().minusSeconds(5)))
                .claim("nonce", "abc")
                .claim("https://purl.imsglobal.org/spec/lti/claim/version", "1.3.0")
                .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", "LtiResourceLinkRequest")
                .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id", "dep-1")
                .build();
        when(processor.process(anyString(), isNull())).thenReturn(claims);

        assertThrows(IllegalArgumentException.class, () -> validator.validate("token", "different"));
    }

    @Test
    void validate_badAudienceThrows() throws Exception {
        LtiJwtValidator validator = new LtiJwtValidator("issuer", "client", "http://example.com/jwks", 60);
        ReflectionTestUtils.setField(validator, "processor", processor);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("issuer")
                .audience("other-client")
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .issueTime(Date.from(Instant.now().minusSeconds(5)))
                .claim("nonce", "ok")
                .claim("https://purl.imsglobal.org/spec/lti/claim/version", "1.3.0")
                .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", "LtiResourceLinkRequest")
                .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id", "dep-1")
                .build();
        when(processor.process(anyString(), isNull())).thenReturn(claims);

        assertThrows(IllegalArgumentException.class, () -> validator.validate("token", "ok"));
    }

    @Test
    void validate_missingRequiredClaimThrows() throws Exception {
        LtiJwtValidator validator = new LtiJwtValidator("issuer", "client", "http://example.com/jwks", 60);
        ReflectionTestUtils.setField(validator, "processor", processor);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("issuer")
                .audience("client")
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .issueTime(Date.from(Instant.now().minusSeconds(5)))
                .claim("nonce", "ok")
                .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", "LtiResourceLinkRequest")
                .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id", "dep-1")
                .build(); // missing version
        when(processor.process(anyString(), isNull())).thenReturn(claims);

        assertThrows(IllegalArgumentException.class, () -> validator.validate("token", "ok"));
    }
}
