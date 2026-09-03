package com.elgourmat.careflow.adapter.out.persistence;

import com.elgourmat.careflow.adapter.out.persistence.entite.ClaimAttachmentEntity;
import com.elgourmat.careflow.adapter.out.persistence.entite.ClaimAttachmentJpaRepository;
import com.elgourmat.careflow.adapter.out.persistence.mapper.AttachmentEntityMapper;
import com.elgourmat.careflow.application.port.out.AttachmentRepository;
import com.elgourmat.careflow.domain.attachment.ClaimAttachment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class AttachmentPersistenceAdapter implements AttachmentRepository {

    private final ClaimAttachmentJpaRepository jpaRepository;
    private final AttachmentEntityMapper mapper;

    public AttachmentPersistenceAdapter(ClaimAttachmentJpaRepository jpaRepository,
                                        AttachmentEntityMapper mapper) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public ClaimAttachment save(ClaimAttachment attachment) {
        ClaimAttachmentEntity saved = jpaRepository.save(mapper.toEntity(attachment));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ClaimAttachment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ClaimAttachment> findByClaimId(UUID claimId) {
        return mapper.toDomains(jpaRepository.findByClaimIdOrderByUploadedAtDesc(claimId));
    }

    @Override
    public List<ClaimAttachment> findAll() {
        return mapper.toDomains(jpaRepository.findAllByOrderByUploadedAtDesc());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
