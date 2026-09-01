package com.elgourmat.careflow.domain.rules;

import com.elgourmat.careflow.domain.Claim;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ClaimRulesEngine {

    private final List<ClaimRule> rules;

    public ClaimRulesEngine(List<ClaimRule> rules) {
        Objects.requireNonNull(rules, "rules is required");
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("rules must not be empty");
        }
        this.rules = List.copyOf(rules);
    }

    public Decision evaluate(Claim claim) {
        Objects.requireNonNull(claim, "claim is required");
        return rules.stream()
                .map(rule -> rule.apply(claim))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No rule matched. Ensure a terminal rule (e.g. DefaultPendingRule) is registered."));
    }
}
