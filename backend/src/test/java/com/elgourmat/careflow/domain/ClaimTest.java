package com.elgourmat.careflow.domain;

import com.elgourmat.careflow.domain.exception.InvalidClaimDateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-01T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final Money TEN_EUROS = new Money(new BigDecimal("10.00"), "EUR");

    @Test
    void newSubmission_initializes_status_to_pending() {
        Claim claim = Claim.newSubmission("patient-42", CareType.DENTAL, TEN_EUROS,
                LocalDate.of(2026, 8, 15), FIXED_CLOCK);

        assertThat(claim.status()).isEqualTo(ClaimStatus.PENDING);
        assertThat(claim.decisionReason()).isNull();
        assertThat(claim.decidedAt()).isNull();
        assertThat(claim.submittedAt()).isEqualTo(FIXED_NOW);
        assertThat(claim.id()).isNotNull();
    }

    @Test
    void newSubmission_accepts_care_date_equal_to_today() {
        LocalDate today = LocalDate.now(FIXED_CLOCK);

        Claim claim = Claim.newSubmission("patient-42", CareType.DENTAL, TEN_EUROS, today, FIXED_CLOCK);

        assertThat(claim.careDate()).isEqualTo(today);
    }

    @Test
    void newSubmission_rejects_future_care_date() {
        LocalDate future = LocalDate.now(FIXED_CLOCK).plusDays(1);

        assertThatThrownBy(() -> Claim.newSubmission("patient-42", CareType.DENTAL, TEN_EUROS, future, FIXED_CLOCK))
                .isInstanceOf(InvalidClaimDateException.class)
                .hasMessageContaining("future");
    }

    @Test
    void decide_returns_new_instance_and_preserves_identity() {
        Claim submitted = Claim.newSubmission("patient-42", CareType.DENTAL, TEN_EUROS,
                LocalDate.of(2026, 8, 15), FIXED_CLOCK);
        Instant laterInstant = FIXED_NOW.plusMillis(123);
        Clock laterClock = Clock.fixed(laterInstant, ZoneOffset.UTC);

        Claim decided = submitted.decide(ClaimStatus.APPROVED, "auto", laterClock);

        assertThat(decided).isNotSameAs(submitted);
        assertThat(decided.id()).isEqualTo(submitted.id());
        assertThat(decided.patientId()).isEqualTo(submitted.patientId());
        assertThat(decided.submittedAt()).isEqualTo(submitted.submittedAt());
        assertThat(decided.status()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(decided.decisionReason()).isEqualTo("auto");
        assertThat(decided.decidedAt()).isEqualTo(laterInstant);
    }

    @Test
    void decide_does_not_mutate_original() {
        Claim submitted = Claim.newSubmission("patient-42", CareType.DENTAL, TEN_EUROS,
                LocalDate.of(2026, 8, 15), FIXED_CLOCK);

        submitted.decide(ClaimStatus.REJECTED, "nope", FIXED_CLOCK);

        assertThat(submitted.status()).isEqualTo(ClaimStatus.PENDING);
        assertThat(submitted.decidedAt()).isNull();
        assertThat(submitted.decisionReason()).isNull();
    }

    @Test
    void constructor_rejects_blank_patientId() {
        assertThatThrownBy(() -> Claim.newSubmission("  ", CareType.DENTAL, TEN_EUROS,
                LocalDate.of(2026, 8, 15), FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("patientId");
    }
}
