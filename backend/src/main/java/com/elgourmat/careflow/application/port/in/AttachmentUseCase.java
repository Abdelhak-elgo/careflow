package com.elgourmat.careflow.application.port.in;

import com.elgourmat.careflow.domain.attachment.ClaimAttachment;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface AttachmentUseCase {

    ClaimAttachment upload(UploadAttachmentCommand command);

    ClaimAttachment getById(UUID attachmentId);

    List<ClaimAttachment> listByClaim(UUID claimId);

    List<ClaimAttachment> listAll();

    DownloadResult download(UUID attachmentId);

    void delete(UUID attachmentId);

    ClaimAttachment rename(RenameAttachmentCommand command);

    record UploadAttachmentCommand(
            UUID claimId,
            String originalName,
            String contentType,
            long sizeBytes,
            InputStream data,
            String uploadedBy
    ) {
    }

    record RenameAttachmentCommand(
            UUID attachmentId,
            String newName
    ) {
    }

    record DownloadResult(
            ClaimAttachment attachment,
            InputStream stream
    ) {
    }
}
