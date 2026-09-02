package com.elgourmat.careflow.adapter.out.persistence;

import com.elgourmat.careflow.adapter.out.persistence.entite.AuditLogEntity;
import com.elgourmat.careflow.adapter.out.persistence.entite.AuditLogJpaRepository;
import com.elgourmat.careflow.application.port.out.AuditPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
public class AuditLogAdapter implements AuditPort {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAdapter.class);
    private static final String SYSTEM = "system";

    private final AuditLogJpaRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock;

    public AuditLogAdapter(AuditLogJpaRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public void record(String action, String entityType, String entityId, Map<String, Object> details) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(UUID.randomUUID());
        entity.setOccurredAt(Instant.now(clock));
        entity.setActor(resolveActor());
        entity.setAction(action);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setDetails(serialize(details));
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntity> search(String entityType, String entityId, String actor, Pageable pageable) {
        return repository.search(entityType, entityId, actor, pageable);
    }

    private static String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return SYSTEM;
        }
        return auth.getName();
    }

    private String serialize(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit details, storing null", e);
            return null;
        }
    }
}
