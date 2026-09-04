package com.elgourmat.careflow.adapter.out.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

@Component
public class IdempotencyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);
    private static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyKeyStore store;
    private final Clock clock;

    public IdempotencyCleanupJob(IdempotencyKeyStore store, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1M")
    public void purgeExpired() {
        int removed = store.deleteOlderThan(clock.instant().minus(TTL));
        if (removed > 0) {
            log.info("Purged {} expired idempotency keys (TTL={}h)", removed, TTL.toHours());
        }
    }
}
