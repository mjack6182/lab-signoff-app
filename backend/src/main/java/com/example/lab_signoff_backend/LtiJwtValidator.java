package com.example.lab_signoff_backend;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * LTI JWT validator for authenticating LTI 1.3 launch requests.
 *
 * This component validates JWT tokens from Canvas LMS using the LTI 1.3 specification.
 * It verifies token signatures using JWKS, validates standard claims (issuer, audience,
 * expiration), and checks LTI-specific claims required by the specification.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
// @Component
public class LtiJwtValidator {
    private final String issuer;
    private final String clientId;
    private final long clockSkew;
    private final ConfigurableJWTProcessor<SecurityContext> processor;

    /**
     * Constructs an LtiJwtValidator with configuration from application properties.
     *
     * @param issuer    The expected issuer (Canvas instance URL)
     * @param clientId  The LTI client ID registered with Canvas
     * @param jwksUrl   The URL to Canvas's JWKS endpoint for key verification
     * @param clockSkew Allowed clock skew in seconds for time-based validations (default: 120)
     * @throws MalformedURLException if the JWKS URL is invalid
     */
    public LtiJwtValidator(
            @Value("${lti.issuer}") String issuer,
            @Value("${lti.client-id}") String clientId,
            @Value("${lti.jwks-url}") String jwksUrl,
            @Value("${lti.allowed-clock-skew-seconds:120}") long clockSkew
    ) throws MalformedURLException {
        this.issuer = Objects.requireNonNull(issuer);
        this.clientId = Objects.requireNonNull(clientId);
        this.clockSkew = clockSkew;

        // Remote JWKS source (Canvas rotates keys; Nimbus handles caching & kid selection)
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(jwksUrl));

        // Accept the RS* family by default (Canvas uses RS256)
        var keySelector = new JWSAlgorithmFamilyJWSKeySelector<>(com.nimbusds.jose.JWSAlgorithm.Family.RSA, keySource);

        this.processor = new DefaultJWTProcessor<>();
        this.processor.setJWSKeySelector(keySelector);
        // Optional: set clock-skew handler by wrapping time checks yourself (below)
    }

    /**
     * Validates an LTI ID token and returns the claims if valid.
     *
     * Performs the following validations:
     * - Signature verification using JWKS
     * - Issuer and audience (client_id) validation
     * - Expiration and issue time checks with clock skew allowance
     * - Nonce validation for replay attack prevention
     * - LTI-required claims validation (version, message_type, deployment_id)
     *
     * @param idToken       The JWT ID token from the LTI launch
     * @param expectedNonce The nonce value expected in the token
     * @return The validated JWT claims set
     * @throws ParseException         if the token cannot be parsed
     * @throws JOSEException         if signature verification fails
     * @throws IllegalArgumentException if any validation check fails
     * @throws BadJOSEException      if the token structure is invalid
     */
    public JWTClaimsSet validate(String idToken, String expectedNonce) throws ParseException, JOSEException, IllegalArgumentException, BadJOSEException {
        var claims = processor.process(idToken, null);

        // ---- Standard claim checks ----
        if (!issuer.equals(claims.getIssuer()))
            throw new IllegalArgumentException("Bad issuer: " + claims.getIssuer());

        List<String> aud = claims.getAudience();
        if (aud == null || aud.stream().noneMatch(a -> a.equals(clientId)))
            throw new IllegalArgumentException("Bad audience (client_id): " + aud);

        var now = Instant.now().getEpochSecond();
        var exp = claims.getExpirationTime() != null ? claims.getExpirationTime().toInstant().getEpochSecond() : 0;
        var iat = claims.getIssueTime() != null ? claims.getIssueTime().toInstant().getEpochSecond() : 0;

        if (exp == 0 || now > exp + clockSkew)
            throw new IllegalArgumentException("Token expired");

        if (iat == 0 || iat - clockSkew > now)
            throw new IllegalArgumentException("Token not valid yet");

        // ---- Nonce check ----
        String nonce = (String) claims.getClaim("nonce");
        if (expectedNonce != null && !expectedNonce.equals(nonce))
            throw new IllegalArgumentException("Nonce mismatch");

        // ---- LTI required claims (basic) ----
        mustHave(claims, "https://purl.imsglobal.org/spec/lti/claim/version");
        mustHave(claims, "https://purl.imsglobal.org/spec/lti/claim/message_type");
        mustHave(claims, "https://purl.imsglobal.org/spec/lti/claim/deployment_id");

        return claims;
    }

    /**
     * Validates that a required claim exists in the JWT claims set.
     *
     * @param c    The JWT claims set
     * @param name The name of the required claim
     * @throws IllegalArgumentException if the claim is missing
     */
    private static void mustHave(JWTClaimsSet c, String name) {
        if (c.getClaim(name) == null) throw new IllegalArgumentException("Missing claim: " + name);
    }
}

