package com.elgourmat.careflow.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditResponse(
        UUID id,
        Instant occurredAt,
        String actor,
        String action,
        String entityType,
        String entityId,
        String details
) {
}
