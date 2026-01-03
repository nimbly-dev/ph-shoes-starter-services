package com.nimbly.phshoesbackend.commons.core.security.jwt;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.util.StringUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.Verification;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class JwtTokenService {

    private final JwtSecurityProperties properties;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenService(JwtSecurityProperties properties) {
        this.properties = properties;
        if (!StringUtils.hasText(properties.getSecret())) {
            throw new IllegalStateException("phshoes.security.jwt.secret must be configured");
        }
        this.algorithm = Algorithm.HMAC256(properties.getSecret());
        Verification verification = JWT.require(algorithm);
        if (StringUtils.hasText(properties.getIssuer())) {
            verification.withIssuer(properties.getIssuer());
        }
        if (StringUtils.hasText(properties.getAudience())) {
            verification.withAudience(properties.getAudience().split(","));
        }
        this.verifier = verification.build();
    }

    public String issueAccessToken(String userId, String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getAccessTtlSeconds());
        var builder = JWT.create()
                .withIssuer(properties.getIssuer())
                .withSubject(userId)
                .withClaim("email", email)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .withJWTId(UUID.randomUUID().toString());
        if (StringUtils.hasText(properties.getAudience())) {
            builder.withAudience(properties.getAudience().split(","));
        }
        return builder.sign(algorithm);
    }

    public DecodedJWT parseAccess(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException ex) {
            throw new JwtVerificationException(ex.getMessage(), ex);
        }
    }

    public String userIdFromAuthorizationHeader(String header) {
        String token = extractToken(header);
        return parseAccess(token).getSubject();
    }

    public long getAccessTtlSeconds() {
        return properties.getAccessTtlSeconds();
    }

    public String getHeaderName() {
        return StringUtils.hasText(properties.getHeaderName()) ? properties.getHeaderName() : "Authorization";
    }

    public String extractToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw new JwtVerificationException("Missing Authorization header", null);
        }
        String prefix = properties.getHeaderPrefix();
        if (StringUtils.hasText(prefix)) {
            if (!authorizationHeader.startsWith(prefix)) {
                throw new JwtVerificationException("Invalid Authorization header format", null);
            }
            return authorizationHeader.substring(prefix.length()).trim();
        }
        return authorizationHeader.trim();
    }
}
