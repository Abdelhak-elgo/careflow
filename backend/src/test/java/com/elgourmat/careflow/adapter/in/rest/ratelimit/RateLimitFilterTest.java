package com.elgourmat.careflow.adapter.in.rest.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        RateLimitProperties props = new RateLimitProperties(3, Duration.ofMinutes(1));
        TokenBucketRegistry registry = new TokenBucketRegistry(props);
        filter = new RateLimitFilter(registry, props);
    }

    @Test
    void allows_up_to_capacity_then_blocks_with_429_and_retry_after() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = perform("patient-quota-a");
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("3");
        }

        MockHttpServletResponse blocked = perform("patient-quota-a");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentType()).isEqualTo("application/problem+json");
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
        assertThat(blocked.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(blocked.getContentAsString()).contains("\"title\": \"Too Many Requests\"");
        assertThat(blocked.getContentAsString()).contains("\"status\": 429");
    }

    @Test
    void buckets_are_isolated_per_user() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(perform("patient-quota-b").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse different = perform("patient-quota-c");
        assertThat(different.getStatus()).isEqualTo(200);
        assertThat(different.getHeader("X-RateLimit-Remaining")).isEqualTo("2");
    }

    @Test
    void falls_back_to_ip_when_no_user_header() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = performWithIp("10.0.0.1");
            assertThat(response.getStatus()).isEqualTo(200);
        }
        assertThat(performWithIp("10.0.0.1").getStatus()).isEqualTo(429);
        assertThat(performWithIp("10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void bypasses_non_matching_methods_or_paths() throws Exception {
        MockHttpServletResponse getResponse = performGet("patient-quota-d");
        assertThat(getResponse.getStatus()).isEqualTo(200);
        assertThat(getResponse.getHeader("X-RateLimit-Limit")).isNull();
    }

    private MockHttpServletResponse perform(String userId) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/claims");
        request.addHeader("X-User-Id", userId);
        return dispatch(request);
    }

    private MockHttpServletResponse performGet(String userId) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/claims");
        request.addHeader("X-User-Id", userId);
        return dispatch(request);
    }

    private MockHttpServletResponse performWithIp(String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/claims");
        request.setRemoteAddr(ip);
        return dispatch(request);
    }

    private MockHttpServletResponse dispatch(HttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        // MockFilterChain no-op → status stays at MockHttpServletResponse default (200)
        // If the RateLimitFilter short-circuits (429), the chain is never invoked
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
