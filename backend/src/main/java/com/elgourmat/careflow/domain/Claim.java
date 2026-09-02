package com.elgourmat.careflow.domain;

import com.elgourmat.careflow.domain.exception.IllegalClaimStateException;
import com.elgourmat.careflow.domain.exception.InvalidClaimDateException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record Claim(
        UUID id,
        String patientId,
        CareType careType,
        Money money,
        LocalDate careDate,
        ClaimStatus status,
        String decisionReason,
        Instant submittedAt,
        Instant decidedAt
) {

    public Claim {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(patientId, "patientId is required");
        if (patientId.isBlank()) {
            throw new IllegalArgumentException("patientId must not be blank");
        }
        Objects.requireNonNull(careType, "careType is required");
        Objects.requireNonNull(money, "money is required");
        Objects.requireNonNull(careDate, "careDate is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(submittedAt, "submittedAt is required");
    }

    public static Claim newSubmission(
            String patientId,
            CareType careType,
            Money money,
            LocalDate careDate,
            Clock clock
    ) {
        Objects.requireNonNull(clock, "clock is required");
        Objects.requireNonNull(careDate, "careDate is required");
        LocalDate today = LocalDate.now(clock);
        if (careDate.isAfter(today)) {
            throw new InvalidClaimDateException("careDate must not be in the future, was: " + careDate);
        }
        return new Claim(
                UUID.randomUUID(),
                patientId,
                careType,
                money,
                careDate,
                ClaimStatus.PENDING,
                null,
                Instant.now(clock),
                null
        );
    }

    public Claim decide(ClaimStatus newStatus, String reason, Clock clock) {
        Objects.requireNonNull(newStatus, "newStatus is required");
        Objects.requireNonNull(clock, "clock is required");
        return new Claim(
                id,
                patientId,
                careType,
                money,
                careDate,
                newStatus,
                reason,
                submittedAt,
                Instant.now(clock)
        );
    }

    private static final Set<ClaimStatus> MANUAL_DECISION_TARGETS =
            Set.of(ClaimStatus.APPROVED, ClaimStatus.REJECTED);

    public Claim manualDecision(ClaimStatus newStatus, String reason, Clock clock) {
        Objects.requireNonNull(newStatus, "newStatus is required");
        Objects.requireNonNull(reason, "reason is required");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank for a manual decision");
        }
        if (status != ClaimStatus.PENDING) {
            throw new IllegalClaimStateException(id, status,
                    "Claim " + id + " is already " + status + " and cannot be re-decided");
        }
        if (!MANUAL_DECISION_TARGETS.contains(newStatus)) {
            throw new IllegalClaimStateException(id, status,
                    "Manual decision must target APPROVED or REJECTED, was " + newStatus);
        }
        return decide(newStatus, reason, clock);
    }
}
