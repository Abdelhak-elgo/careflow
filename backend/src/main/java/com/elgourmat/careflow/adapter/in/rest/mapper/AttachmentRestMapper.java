package com.elgourmat.careflow.adapter.in.rest.mapper;

import com.elgourmat.careflow.adapter.in.rest.dto.AttachmentResponse;
import com.elgourmat.careflow.domain.attachment.ClaimAttachment;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttachmentRestMapper {

    AttachmentResponse toResponse(ClaimAttachment attachment);

    List<AttachmentResponse> toResponses(List<ClaimAttachment> attachments);
}
