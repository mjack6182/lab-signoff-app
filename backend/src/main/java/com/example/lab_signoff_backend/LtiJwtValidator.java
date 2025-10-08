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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@ConditionalOnProperty(name = "lti.validation-enabled", havingValue = "true")
@Component
public class LtiJwtValidator {
    private final String issuer;
    private final String clientId;
    private final long clockSkew;
    private final ConfigurableJWTProcessor<SecurityContext> processor;

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

    private static void mustHave(JWTClaimsSet c, String name) {
        if (c.getClaim(name) == null) throw new IllegalArgumentException("Missing claim: " + name);
    }
}

