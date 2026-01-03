package com.nimbly.phshoesbackend.commons.core.security.jwt;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_setsAuthenticationWhenTokenValid() throws Exception {
        JwtSecurityProperties properties = new JwtSecurityProperties();
        properties.setSecret("secret");
        JwtTokenService tokenService = new JwtTokenService(properties);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, properties);

        String token = tokenService.issueAccessToken("user-1", "user@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(tokenService.getHeaderName(), "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_skipsWhenPathExcluded() throws Exception {
        JwtSecurityProperties properties = new JwtSecurityProperties();
        properties.setSecret("secret");
        properties.getSkipPaths().add("/public/**");
        JwtTokenService tokenService = new JwtTokenService(properties);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, properties);

        String token = tokenService.issueAccessToken("user-1", "user@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public/health");
        request.addHeader(tokenService.getHeaderName(), "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
