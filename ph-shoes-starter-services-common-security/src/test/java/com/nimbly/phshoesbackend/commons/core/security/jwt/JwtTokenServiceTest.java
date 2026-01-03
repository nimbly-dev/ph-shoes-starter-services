package com.nimbly.phshoesbackend.commons.core.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    @Test
    void constructor_requiresSecret() {
        JwtSecurityProperties properties = new JwtSecurityProperties();
        properties.setSecret("");

        assertThrows(IllegalStateException.class, () -> new JwtTokenService(properties));
    }

    @Test
    void issueAccessToken_andParseAccess() {
        JwtTokenService service = new JwtTokenService(buildProperties());

        String token = service.issueAccessToken("user-1", "user@example.com");

        assertNotNull(token);
        assertEquals("user-1", service.parseAccess(token).getSubject());
        assertEquals("user@example.com", service.parseAccess(token).getClaim("email").asString());
    }

    @Test
    void extractToken_requiresPrefixWhenConfigured() {
        JwtSecurityProperties properties = buildProperties();
        properties.setHeaderPrefix("Bearer ");
        JwtTokenService service = new JwtTokenService(properties);

        assertEquals("token-value", service.extractToken("Bearer token-value"));
        assertThrows(JwtVerificationException.class, () -> service.extractToken("token-value"));
    }

    private static JwtSecurityProperties buildProperties() {
        JwtSecurityProperties properties = new JwtSecurityProperties();
        properties.setSecret("test-secret");
        properties.setIssuer("ph-shoes");
        return properties;
    }
}
