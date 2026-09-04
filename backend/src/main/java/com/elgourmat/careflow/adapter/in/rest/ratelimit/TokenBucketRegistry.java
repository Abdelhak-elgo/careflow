package com.elgourmat.careflow.adapter.in.rest.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TokenBucketRegistry {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RateLimitProperties properties;

    public TokenBucketRegistry(RateLimitProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    public Bucket bucket(String key) {
        Objects.requireNonNull(key, "key is required");
        return buckets.computeIfAbsent(key, this::newBucket);
    }

    private Bucket newBucket(String ignored) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(properties.capacity())
                .refillGreedy(properties.capacity(), properties.refillPeriod())
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
