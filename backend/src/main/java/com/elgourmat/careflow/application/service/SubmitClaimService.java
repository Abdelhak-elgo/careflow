package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.SubmitClaimUseCase;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.Money;
import com.elgourmat.careflow.domain.rules.ClaimRulesEngine;
import com.elgourmat.careflow.domain.rules.Decision;

import java.time.Clock;
import java.util.Objects;

public class SubmitClaimService implements SubmitClaimUseCase {

    private final ClaimRulesEngine rulesEngine;
    private final ClaimRepository claimRepository;
    private final Clock clock;

    public SubmitClaimService(ClaimRulesEngine rulesEngine, ClaimRepository claimRepository, Clock clock) {
        this.rulesEngine = Objects.requireNonNull(rulesEngine);
        this.claimRepository = Objects.requireNonNull(claimRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Claim submit(SubmitClaimCommand command) {
        Objects.requireNonNull(command, "command is required");
        Money money = new Money(command.amount(), command.currency());
        Claim submitted = Claim.newSubmission(
                command.patientId(),
                command.careType(),
                money,
                command.careDate(),
                clock
        );
        Decision decision = rulesEngine.evaluate(submitted);
        Claim decided = submitted.decide(decision.status(), decision.reason(), clock);
        return claimRepository.save(decided);
    }
}
