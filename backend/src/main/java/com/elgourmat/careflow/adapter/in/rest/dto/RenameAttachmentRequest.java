package com.elgourmat.careflow.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameAttachmentRequest(
        @NotBlank @Size(max = 255) String originalName
) {
}
