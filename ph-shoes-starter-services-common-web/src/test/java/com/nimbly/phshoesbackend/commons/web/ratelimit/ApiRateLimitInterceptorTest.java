package com.nimbly.phshoesbackend.commons.web.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.nimbly.phshoesbackend.commons.core.ratelimit.ApiRateLimitException;
import com.nimbly.phshoesbackend.commons.core.ratelimit.ApiRateLimiter;
import com.nimbly.phshoesbackend.commons.core.ratelimit.RateLimitRequestContext;

class ApiRateLimitInterceptorTest {

    @Test
    void preHandle_allowsWhenLimiterAccepts() throws Exception {
        ApiRateLimiter limiter = Mockito.mock(ApiRateLimiter.class);
        ApiRateLimitInterceptor interceptor = new ApiRateLimitInterceptor(limiter);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("X-Forwarded-For", "10.0.0.1");
        request.addHeader("X-User-Id", "user-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        ArgumentCaptor<RateLimitRequestContext> captor = ArgumentCaptor.forClass(RateLimitRequestContext.class);
        verify(limiter).verifyRequest(captor.capture());
        assertEquals("10.0.0.1", captor.getValue().getIpAddress());
        assertEquals("user-1", captor.getValue().getUserId());
    }

    @Test
    void preHandle_blocksWhenLimiterThrows() throws Exception {
        ApiRateLimiter limiter = Mockito.mock(ApiRateLimiter.class);
        doThrow(new ApiRateLimitException("limit")).when(limiter).verifyRequest(any());
        ApiRateLimitInterceptor interceptor = new ApiRateLimitInterceptor(limiter);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(429, response.getStatus());
    }
}
