package com.elgourmat.careflow.adapter.in.rest.dto;

import com.elgourmat.careflow.domain.CareType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitClaimRequest(
        @NotBlank String patientId,
        @NotNull CareType careType,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than zero") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3, message = "currency must be an ISO 4217 code (3 letters)") String currency,
        @NotNull @PastOrPresent(message = "careDate must not be in the future") LocalDate careDate
) {
}
