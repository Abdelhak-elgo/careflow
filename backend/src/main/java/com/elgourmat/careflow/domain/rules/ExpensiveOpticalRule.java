package com.elgourmat.careflow.domain.rules;

import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;

import java.math.BigDecimal;
import java.util.Optional;

public class ExpensiveOpticalRule implements ClaimRule {

    private static final BigDecimal THRESHOLD = new BigDecimal("500");
    private static final String CURRENCY = "EUR";
    private static final String REASON = "Optical claim above 500 EUR requires manual pre-authorization";

    @Override
    public Optional<Decision> apply(Claim claim) {
        if (claim.careType() != CareType.OPTICAL) {
            return Optional.empty();
        }
        if (!claim.money().isCurrency(CURRENCY)) {
            return Optional.empty();
        }
        if (claim.money().isGreaterThan(THRESHOLD)) {
            return Optional.of(new Decision(ClaimStatus.REJECTED, REASON));
        }
        return Optional.empty();
    }
}
