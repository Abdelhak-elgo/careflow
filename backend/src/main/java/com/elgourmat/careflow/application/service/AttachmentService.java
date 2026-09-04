package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.AttachmentUseCase;
import com.elgourmat.careflow.application.port.out.AttachmentRepository;
import com.elgourmat.careflow.application.port.out.AuditPort;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.application.port.out.FileStoragePort;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import com.elgourmat.careflow.domain.attachment.ClaimAttachment;
import com.elgourmat.careflow.domain.exception.AttachmentNotFoundException;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;
import com.elgourmat.careflow.domain.exception.IllegalClaimStateException;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AttachmentService implements AttachmentUseCase {

    private final AttachmentRepository attachments;
    private final ClaimRepository claims;
    private final FileStoragePort storage;
    private final AuditPort audit;
    private final Clock clock;

    public AttachmentService(
            AttachmentRepository attachments,
            ClaimRepository claims,
            FileStoragePort storage,
            AuditPort audit,
            Clock clock
    ) {
        this.attachments = Objects.requireNonNull(attachments);
        this.claims = Objects.requireNonNull(claims);
        this.storage = Objects.requireNonNull(storage);
        this.audit = Objects.requireNonNull(audit);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public ClaimAttachment upload(UploadAttachmentCommand command) {
        Objects.requireNonNull(command, "command is required");
        Claim claim = claims.findById(command.claimId())
                .orElseThrow(() -> new ClaimNotFoundException(command.claimId()));
        requirePending(claim, "attachments can only be added while the claim is PENDING");

        UUID id = UUID.randomUUID();
        String objectKey = "attachments/" + command.claimId() + "/" + id;
        storage.put(objectKey, command.data(), command.sizeBytes(), command.contentType());

        ClaimAttachment attachment = new ClaimAttachment(
                id,
                command.claimId(),
                objectKey,
                command.originalName(),
                command.contentType(),
                command.sizeBytes(),
                command.uploadedBy(),
                Instant.now(clock)
        );
        ClaimAttachment persisted = attachments.save(attachment);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("claimId", persisted.claimId().toString());
        details.put("originalName", persisted.originalName());
        details.put("contentType", persisted.contentType());
        details.put("sizeBytes", persisted.sizeBytes());
        audit.record("ATTACHMENT_UPLOADED", "ATTACHMENT", persisted.id().toString(), details);

        return persisted;
    }

    @Override
    public ClaimAttachment getById(UUID attachmentId) {
        Objects.requireNonNull(attachmentId, "attachmentId is required");
        return attachments.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));
    }

    @Override
    public List<ClaimAttachment> listByClaim(UUID claimId) {
        Objects.requireNonNull(claimId, "claimId is required");
        return attachments.findByClaimId(claimId);
    }

    @Override
    public List<ClaimAttachment> listAll() {
        return attachments.findAll();
    }

    @Override
    public DownloadResult download(UUID attachmentId) {
        Objects.requireNonNull(attachmentId, "attachmentId is required");
        ClaimAttachment attachment = attachments.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));
        return new DownloadResult(attachment, storage.get(attachment.objectKey()));
    }

    @Override
    public void delete(UUID attachmentId) {
        Objects.requireNonNull(attachmentId, "attachmentId is required");
        ClaimAttachment attachment = attachments.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));
        Claim claim = claims.findById(attachment.claimId())
                .orElseThrow(() -> new ClaimNotFoundException(attachment.claimId()));
        requirePending(claim, "attachments can only be removed while the claim is PENDING");

        storage.delete(attachment.objectKey());
        attachments.deleteById(attachmentId);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("claimId", attachment.claimId().toString());
        details.put("originalName", attachment.originalName());
        audit.record("ATTACHMENT_DELETED", "ATTACHMENT", attachmentId.toString(), details);
    }

    private static void requirePending(Claim claim, String reason) {
        if (claim.status() != ClaimStatus.PENDING) {
            throw new IllegalClaimStateException(claim.id(), claim.status(),
                    "Claim " + claim.id() + " is " + claim.status() + " — " + reason);
        }
    }

    @Override
    public ClaimAttachment rename(RenameAttachmentCommand command) {
        Objects.requireNonNull(command, "command is required");
        ClaimAttachment current = attachments.findById(command.attachmentId())
                .orElseThrow(() -> new AttachmentNotFoundException(command.attachmentId()));
        ClaimAttachment renamed = current.withOriginalName(command.newName());
        ClaimAttachment persisted = attachments.save(renamed);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("claimId", persisted.claimId().toString());
        details.put("oldName", current.originalName());
        details.put("newName", persisted.originalName());
        audit.record("ATTACHMENT_RENAMED", "ATTACHMENT", persisted.id().toString(), details);

        return persisted;
    }
}
