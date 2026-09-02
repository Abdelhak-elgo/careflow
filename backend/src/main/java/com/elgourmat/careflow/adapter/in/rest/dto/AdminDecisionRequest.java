package com.elgourmat.careflow.adapter.in.rest.dto;

import com.elgourmat.careflow.domain.ClaimStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminDecisionRequest(
        @NotNull ClaimStatus decision,
        @NotBlank @Size(max = 500, message = "reason must be at most 500 characters") String reason
) {
}
