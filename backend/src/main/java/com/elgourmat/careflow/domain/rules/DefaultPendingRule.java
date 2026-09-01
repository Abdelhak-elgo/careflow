package com.elgourmat.careflow.domain.rules;

import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;

import java.util.Optional;

public class DefaultPendingRule implements ClaimRule {

    private static final String REASON = "Sent to manual review";

    @Override
    public Optional<Decision> apply(Claim claim) {
        return Optional.of(new Decision(ClaimStatus.PENDING, REASON));
    }
}
