package com.elgourmat.careflow.domain.attachment;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ClaimAttachment(
        UUID id,
        UUID claimId,
        String objectKey,
        String originalName,
        String contentType,
        long sizeBytes,
        String uploadedBy,
        Instant uploadedAt
) {

    public ClaimAttachment {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(claimId, "claimId is required");
        Objects.requireNonNull(objectKey, "objectKey is required");
        Objects.requireNonNull(originalName, "originalName is required");
        Objects.requireNonNull(contentType, "contentType is required");
        Objects.requireNonNull(uploadedBy, "uploadedBy is required");
        Objects.requireNonNull(uploadedAt, "uploadedAt is required");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
    }

    public ClaimAttachment withOriginalName(String newName) {
        Objects.requireNonNull(newName, "newName is required");
        if (newName.isBlank()) {
            throw new IllegalArgumentException("originalName must not be blank");
        }
        return new ClaimAttachment(id, claimId, objectKey, newName, contentType,
                sizeBytes, uploadedBy, uploadedAt);
    }
}
