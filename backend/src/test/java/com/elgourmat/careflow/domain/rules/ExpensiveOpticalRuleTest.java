package com.elgourmat.careflow.domain.rules;

import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import com.elgourmat.careflow.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ExpensiveOpticalRuleTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);
    private final ExpensiveOpticalRule rule = new ExpensiveOpticalRule();

    @Test
    void rejects_optical_above_500_eur() {
        Claim claim = claimOf(new BigDecimal("500.01"), "EUR", CareType.OPTICAL);

        Optional<Decision> decision = rule.apply(claim);

        assertThat(decision).isPresent();
        assertThat(decision.get().status()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(decision.get().reason()).contains("Optical");
    }

    @Test
    void does_not_match_when_optical_is_exactly_500_eur() {
        Claim claim = claimOf(new BigDecimal("500.00"), "EUR", CareType.OPTICAL);

        assertThat(rule.apply(claim)).isEmpty();
    }

    @Test
    void does_not_match_when_care_type_is_not_optical_even_if_expensive() {
        Claim claim = claimOf(new BigDecimal("1000.00"), "EUR", CareType.DENTAL);

        assertThat(rule.apply(claim)).isEmpty();
    }

    @Test
    void does_not_match_when_currency_is_not_eur() {
        Claim claim = claimOf(new BigDecimal("2000.00"), "USD", CareType.OPTICAL);

        assertThat(rule.apply(claim)).isEmpty();
    }

    private static Claim claimOf(BigDecimal amount, String currency, CareType careType) {
        return Claim.newSubmission("patient-42", careType,
                new Money(amount, currency), LocalDate.of(2026, 8, 15), CLOCK);
    }
}
