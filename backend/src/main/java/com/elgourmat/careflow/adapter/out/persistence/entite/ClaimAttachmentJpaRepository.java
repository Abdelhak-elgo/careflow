package com.elgourmat.careflow.adapter.out.persistence.entite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimAttachmentJpaRepository extends JpaRepository<ClaimAttachmentEntity, UUID> {

    List<ClaimAttachmentEntity> findByClaimIdOrderByUploadedAtDesc(UUID claimId);

    List<ClaimAttachmentEntity> findAllByOrderByUploadedAtDesc();
}
