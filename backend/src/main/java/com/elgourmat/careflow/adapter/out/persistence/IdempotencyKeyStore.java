package com.elgourmat.careflow.adapter.out.persistence;

import com.elgourmat.careflow.adapter.out.persistence.entite.IdempotencyKeyEntity;
import com.elgourmat.careflow.adapter.out.persistence.entite.IdempotencyKeyJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class IdempotencyKeyStore {

    private final IdempotencyKeyJpaRepository repository;
    private final Clock clock;

    public IdempotencyKeyStore(IdempotencyKeyJpaRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> lookup(String key) {
        return repository.findById(key).map(IdempotencyKeyEntity::getClaimId);
    }

    @Transactional
    public void store(String key, UUID claimId) {
        Objects.requireNonNull(key, "key is required");
        Objects.requireNonNull(claimId, "claimId is required");
        if (repository.existsById(key)) {
            return;
        }
        repository.save(new IdempotencyKeyEntity(key, claimId, Instant.now(clock)));
    }

    @Transactional
    public int deleteOlderThan(Instant cutoff) {
        return repository.deleteOlderThan(cutoff);
    }
}
