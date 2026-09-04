package com.elgourmat.careflow.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        UUID claimId,
        String originalName,
        String contentType,
        long sizeBytes,
        String uploadedBy,
        Instant uploadedAt
) {
}
