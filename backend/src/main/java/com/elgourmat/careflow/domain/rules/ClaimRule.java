package com.elgourmat.careflow.domain.rules;

import com.elgourmat.careflow.domain.Claim;

import java.util.Optional;

@FunctionalInterface
public interface ClaimRule {

    Optional<Decision> apply(Claim claim);
}
