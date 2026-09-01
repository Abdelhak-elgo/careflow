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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimRulesEngineTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);

    private final ClaimRulesEngine engine = new ClaimRulesEngine(List.of(
            new SmallAmountRule(),
            new ExpensiveOpticalRule(),
            new DefaultPendingRule()
    ));

    @Test
    void small_optical_amount_is_approved_before_expensive_optical_rule_fires() {
        Claim claim = claimOf(new BigDecimal("50.00"), "EUR", CareType.OPTICAL);

        Decision decision = engine.evaluate(claim);

        assertThat(decision.status()).isEqualTo(ClaimStatus.APPROVED);
    }

    @Test
    void expensive_optical_is_rejected() {
        Claim claim = claimOf(new BigDecimal("800.00"), "EUR", CareType.OPTICAL);

        Decision decision = engine.evaluate(claim);

        assertThat(decision.status()).isEqualTo(ClaimStatus.REJECTED);
    }

    @Test
    void medium_dental_falls_through_to_pending() {
        Claim claim = claimOf(new BigDecimal("300.00"), "EUR", CareType.DENTAL);

        Decision decision = engine.evaluate(claim);

        assertThat(decision.status()).isEqualTo(ClaimStatus.PENDING);
    }

    @Test
    void non_eur_currency_falls_through_to_pending() {
        Claim claim = claimOf(new BigDecimal("50.00"), "USD", CareType.OPTICAL);

        Decision decision = engine.evaluate(claim);

        assertThat(decision.status()).isEqualTo(ClaimStatus.PENDING);
    }

    @Test
    void constructor_rejects_empty_rule_list() {
        assertThatThrownBy(() -> new ClaimRulesEngine(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void evaluate_throws_when_no_rule_matches() {
        ClaimRulesEngine strictEngine = new ClaimRulesEngine(List.of(new SmallAmountRule()));
        Claim claim = claimOf(new BigDecimal("300.00"), "EUR", CareType.DENTAL);

        assertThatThrownBy(() -> strictEngine.evaluate(claim))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Claim claimOf(BigDecimal amount, String currency, CareType careType) {
        return Claim.newSubmission("patient-42", careType,
                new Money(amount, currency), LocalDate.of(2026, 8, 15), CLOCK);
    }
}
