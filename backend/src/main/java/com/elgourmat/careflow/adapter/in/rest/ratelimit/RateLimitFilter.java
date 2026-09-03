package com.elgourmat.careflow.adapter.in.rest.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

public class RateLimitFilter extends OncePerRequestFilter {

    static final String CLAIMS_PATH = "/api/claims";
    private static final java.util.regex.Pattern ATTACHMENTS_UPLOAD_PATH =
            java.util.regex.Pattern.compile("^/api/claims/[0-9a-fA-F-]{36}/attachments/?$");
    static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    static final String HEADER_LIMIT = "X-RateLimit-Limit";

    private final TokenBucketRegistry registry;
    private final RateLimitProperties properties;

    public RateLimitFilter(TokenBucketRegistry registry, RateLimitProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !(CLAIMS_PATH.equals(uri) || ATTACHMENTS_UPLOAD_PATH.matcher(uri).matches());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = resolveKey(request);
        ConsumptionProbe probe = registry.bucket(key).tryConsumeAndReturnRemaining(1);

        response.setHeader(HEADER_LIMIT, String.valueOf(properties.capacity()));

        if (probe.isConsumed()) {
            response.setHeader(HEADER_REMAINING, String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setHeader(HEADER_REMAINING, "0");
        response.getWriter().write("""
                {
                  "type": "about:blank",
                  "title": "Too Many Requests",
                  "status": 429,
                  "detail": "Rate limit exceeded for key '%s'. Retry after %d seconds.",
                  "retryAfterSeconds": %d
                }
                """.formatted(escape(key), retryAfterSeconds, retryAfterSeconds));
    }

    private static String resolveKey(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        String ip = request.getRemoteAddr();
        return "ip:" + (ip == null ? "unknown" : ip);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
