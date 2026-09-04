package com.elgourmat.careflow.domain.exception;

import java.util.UUID;

public class AttachmentNotFoundException extends RuntimeException {

    private final UUID attachmentId;

    public AttachmentNotFoundException(UUID attachmentId) {
        super("Attachment not found: " + attachmentId);
        this.attachmentId = attachmentId;
    }

    public UUID attachmentId() {
        return attachmentId;
    }
}
