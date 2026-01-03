package com.nimbly.phshoesbackend.commons.core.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import com.nimbly.phshoesbackend.commons.core.config.props.ApiRateLimitProperties;
import com.nimbly.phshoesbackend.commons.core.config.props.ApiRateLimitProperties.LimitConfig;
import com.nimbly.phshoesbackend.commons.core.config.props.ApiRateLimitProperties.Route;
import com.nimbly.phshoesbackend.commons.core.api.rate.RateLimitExceededException;

public class InMemoryApiRateLimiter implements ApiRateLimiter {

    private final ApiRateLimitProperties properties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public InMemoryApiRateLimiter(ApiRateLimitProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public void verifyRequest(RateLimitRequestContext context) {
        if (context == null) {
            return;
        }
        Instant now = Instant.now();
        Duration defaultWindow = properties.getDefaultWindow();

        checkLimit("global", properties.getGlobal(), defaultWindow, now, context.getPath());

        if (StringUtils.hasText(context.getIpAddress())) {
            checkLimit("ip:" + context.getIpAddress(), properties.getPerIp(), defaultWindow, now, context.getPath());
        }

        if (StringUtils.hasText(context.getUserId())) {
            checkLimit("user:" + context.getUserId(), properties.getPerUser(), defaultWindow, now, context.getPath());
        }

        applyRouteOverrides(context, now, defaultWindow);
    }

    private void applyRouteOverrides(RateLimitRequestContext context, Instant now, Duration defaultWindow) {
        List<Route> routes = properties.getRoutes();
        if (routes == null || routes.isEmpty() || !StringUtils.hasText(context.getPath()) || !StringUtils.hasText(context.getUserId())) {
            return;
        }

        for (Route route : routes) {
            if (!StringUtils.hasText(route.getPattern())) {
                continue;
            }
            if (pathMatcher.match(route.getPattern(), context.getPath())) {
                LimitConfig perUser = route.getPerUser();
                if (perUser == null) {
                    continue;
                }
                checkLimit("route:" + route.getName() + ":user:" + context.getUserId(),
                        perUser, defaultWindow, now, route.getName());
            }
        }
    }

    private void checkLimit(String key,
                            LimitConfig limitConfig,
                            Duration defaultWindow,
                            Instant now,
                            String routeName) {
        if (limitConfig == null || limitConfig.getLimit() <= 0) {
            return;
        }

        Duration window = limitConfig.getWindow() != null ? limitConfig.getWindow() : defaultWindow;
        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter(now));
        long value = counter.incrementAndGet(window, now);
        if (value > limitConfig.getLimit()) {
            throw new RateLimitExceededException(key, "Rate limit exceeded for " + key + " on " + routeName);
        }
    }

    private static final class WindowCounter {

        private volatile Instant windowStart;

        private final AtomicLong count = new AtomicLong();

        WindowCounter(Instant now) {
            this.windowStart = now;
            this.count.set(0);
        }

        long incrementAndGet(Duration window, Instant now) {
            resetIfExpired(window, now);
            return count.incrementAndGet();
        }

        private synchronized void resetIfExpired(Duration window, Instant now) {
            Instant localStart = this.windowStart;
            if (localStart.plus(window).isBefore(now) || localStart.plus(window).equals(now)) {
                this.windowStart = now;
                this.count.set(0);
            }
        }
    }
}
