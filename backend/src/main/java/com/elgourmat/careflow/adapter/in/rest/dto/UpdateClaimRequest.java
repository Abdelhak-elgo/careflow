package com.elgourmat.careflow.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateClaimRequest(
        @NotBlank @Size(max = 64) String patientId,
        @NotNull @PastOrPresent(message = "careDate must not be in the future") LocalDate careDate
) {
}
