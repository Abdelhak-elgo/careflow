package com.elgourmat.careflow.adapter.out.persistence.mapper;

import com.elgourmat.careflow.adapter.out.persistence.entite.ClaimAttachmentEntity;
import com.elgourmat.careflow.domain.attachment.ClaimAttachment;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttachmentEntityMapper {

    ClaimAttachmentEntity toEntity(ClaimAttachment attachment);

    default ClaimAttachment toDomain(ClaimAttachmentEntity entity) {
        return new ClaimAttachment(
                entity.getId(),
                entity.getClaimId(),
                entity.getObjectKey(),
                entity.getOriginalName(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getUploadedBy(),
                entity.getUploadedAt()
        );
    }

    List<ClaimAttachment> toDomains(List<ClaimAttachmentEntity> entities);
}
