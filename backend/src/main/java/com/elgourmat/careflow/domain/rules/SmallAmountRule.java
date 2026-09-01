package com.elgourmat.careflow.domain.rules;

import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;

import java.math.BigDecimal;
import java.util.Optional;

public class SmallAmountRule implements ClaimRule {

    private static final BigDecimal THRESHOLD = new BigDecimal("100");
    private static final String CURRENCY = "EUR";
    private static final String REASON = "Auto-approval: amount below 100 EUR";

    @Override
    public Optional<Decision> apply(Claim claim) {
        if (!claim.money().isCurrency(CURRENCY)) {
            return Optional.empty();
        }
        if (claim.money().isLessThan(THRESHOLD)) {
            return Optional.of(new Decision(ClaimStatus.APPROVED, REASON));
        }
        return Optional.empty();
    }
}
