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

class DefaultPendingRuleTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);
    private final DefaultPendingRule rule = new DefaultPendingRule();

    @Test
    void always_returns_pending_regardless_of_claim() {
        Claim optical = Claim.newSubmission("patient-42", CareType.OPTICAL,
                new Money(new BigDecimal("10000"), "USD"), LocalDate.of(2026, 8, 15), CLOCK);
        Claim tinyDental = Claim.newSubmission("patient-42", CareType.DENTAL,
                new Money(new BigDecimal("1"), "EUR"), LocalDate.of(2026, 8, 15), CLOCK);

        Optional<Decision> a = rule.apply(optical);
        Optional<Decision> b = rule.apply(tinyDental);

        assertThat(a).isPresent();
        assertThat(a.get().status()).isEqualTo(ClaimStatus.PENDING);
        assertThat(b).isPresent();
        assertThat(b.get().status()).isEqualTo(ClaimStatus.PENDING);
    }
}
