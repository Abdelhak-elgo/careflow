package com.elgourmat.careflow.application.port.out;

import com.elgourmat.careflow.domain.attachment.ClaimAttachment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository {

    ClaimAttachment save(ClaimAttachment attachment);

    Optional<ClaimAttachment> findById(UUID id);

    List<ClaimAttachment> findByClaimId(UUID claimId);

    List<ClaimAttachment> findAll();

    void deleteById(UUID id);
}
