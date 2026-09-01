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

class SmallAmountRuleTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);
    private final SmallAmountRule rule = new SmallAmountRule();

    @Test
    void approves_when_amount_below_100_eur() {
        Claim claim = claimOf(new BigDecimal("99.99"), "EUR", CareType.DENTAL);

        Optional<Decision> decision = rule.apply(claim);

        assertThat(decision).isPresent();
        assertThat(decision.get().status()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(decision.get().reason()).contains("100 EUR");
    }

    @Test
    void does_not_match_when_amount_is_exactly_100_eur() {
        Claim claim = claimOf(new BigDecimal("100.00"), "EUR", CareType.DENTAL);

        assertThat(rule.apply(claim)).isEmpty();
    }

    @Test
    void does_not_match_when_amount_above_100_eur() {
        Claim claim = claimOf(new BigDecimal("100.01"), "EUR", CareType.DENTAL);

        assertThat(rule.apply(claim)).isEmpty();
    }

    @Test
    void does_not_match_when_currency_is_not_eur() {
        Claim claim = claimOf(new BigDecimal("50.00"), "USD", CareType.DENTAL);

        assertThat(rule.apply(claim)).isEmpty();
    }

    private static Claim claimOf(BigDecimal amount, String currency, CareType careType) {
        return Claim.newSubmission("patient-42", careType,
                new Money(amount, currency), LocalDate.of(2026, 8, 15), CLOCK);
    }
}
