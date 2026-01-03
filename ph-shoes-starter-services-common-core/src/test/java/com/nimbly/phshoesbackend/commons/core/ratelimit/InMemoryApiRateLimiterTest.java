package com.nimbly.phshoesbackend.commons.core.ratelimit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.nimbly.phshoesbackend.commons.core.api.rate.RateLimitExceededException;
import com.nimbly.phshoesbackend.commons.core.config.props.ApiRateLimitProperties;

class InMemoryApiRateLimiterTest {

    @Test
    void verifyRequest_allowsNullContext() {
        ApiRateLimitProperties properties = new ApiRateLimitProperties();
        InMemoryApiRateLimiter limiter = new InMemoryApiRateLimiter(properties);

        assertDoesNotThrow(() -> limiter.verifyRequest(null));
    }

    @Test
    void verifyRequest_enforcesGlobalLimit() {
        ApiRateLimitProperties properties = new ApiRateLimitProperties();
        properties.setDefaultWindow(Duration.ofSeconds(30));
        properties.getGlobal().setLimit(1);
        InMemoryApiRateLimiter limiter = new InMemoryApiRateLimiter(properties);

        RateLimitRequestContext context = RateLimitRequestContext.builder()
                .path("/api/test")
                .build();

        limiter.verifyRequest(context);
        assertThrows(RateLimitExceededException.class, () -> limiter.verifyRequest(context));
    }

    @Test
    void verifyRequest_appliesRouteOverrides() {
        ApiRateLimitProperties properties = new ApiRateLimitProperties();
        properties.setDefaultWindow(Duration.ofSeconds(30));
        properties.getGlobal().setLimit(0);
        properties.getPerUser().setLimit(0);

        ApiRateLimitProperties.Route route = new ApiRateLimitProperties.Route();
        route.setName("signup");
        route.setPattern("/api/**");
        route.getPerUser().setLimit(1);
        properties.getRoutes().add(route);

        InMemoryApiRateLimiter limiter = new InMemoryApiRateLimiter(properties);
        RateLimitRequestContext context = RateLimitRequestContext.builder()
                .path("/api/v1/signup")
                .userId("user-1")
                .build();

        limiter.verifyRequest(context);
        assertThrows(RateLimitExceededException.class, () -> limiter.verifyRequest(context));
    }
}
