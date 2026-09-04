package com.elgourmat.careflow.adapter.in.rest.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "careflow.rate-limit")
public record RateLimitProperties(int capacity, Duration refillPeriod) {

    public RateLimitProperties {
        if (capacity <= 0) {
            throw new IllegalArgumentException("careflow.rate-limit.capacity must be > 0, was " + capacity);
        }
        if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("careflow.rate-limit.refill-period must be positive, was " + refillPeriod);
        }
    }
}
