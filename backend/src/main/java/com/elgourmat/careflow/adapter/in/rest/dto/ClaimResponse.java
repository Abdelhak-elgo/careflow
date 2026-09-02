package com.elgourmat.careflow.adapter.in.rest.dto;

import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        String patientId,
        CareType careType,
        BigDecimal amount,
        String currency,
        LocalDate careDate,
        ClaimStatus status,
        String decisionReason,
        Instant submittedAt,
        Instant decidedAt
) {
}
