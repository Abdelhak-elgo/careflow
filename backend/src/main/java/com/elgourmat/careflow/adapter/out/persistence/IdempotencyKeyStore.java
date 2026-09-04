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

    public static final String RESOURCE_CLAIM = "CLAIM";
    public static final String RESOURCE_ATTACHMENT = "ATTACHMENT";

    private final IdempotencyKeyJpaRepository repository;
    private final Clock clock;

    public IdempotencyKeyStore(IdempotencyKeyJpaRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> lookup(String key, String resourceType) {
        Objects.requireNonNull(key, "key is required");
        Objects.requireNonNull(resourceType, "resourceType is required");
        return repository.findById(key)
                .filter(e -> resourceType.equals(e.getResourceType()))
                .map(IdempotencyKeyEntity::getResourceId);
    }

    @Transactional
    public void store(String key, String resourceType, UUID resourceId) {
        Objects.requireNonNull(key, "key is required");
        Objects.requireNonNull(resourceType, "resourceType is required");
        Objects.requireNonNull(resourceId, "resourceId is required");
        if (repository.existsById(key)) {
            return;
        }
        repository.save(new IdempotencyKeyEntity(key, resourceId, resourceType, Instant.now(clock)));
    }

    @Transactional
    public int deleteOlderThan(Instant cutoff) {
        return repository.deleteOlderThan(cutoff);
    }
}
