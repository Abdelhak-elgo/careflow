package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.DecideClaimUseCase;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;

import java.time.Clock;
import java.util.Objects;

public class DecideClaimService implements DecideClaimUseCase {

    private final ClaimRepository claimRepository;
    private final Clock clock;

    public DecideClaimService(ClaimRepository claimRepository, Clock clock) {
        this.claimRepository = Objects.requireNonNull(claimRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Claim decide(DecideClaimCommand command) {
        Objects.requireNonNull(command, "command is required");
        Claim current = claimRepository.findById(command.claimId())
                .orElseThrow(() -> new ClaimNotFoundException(command.claimId()));
        Claim decided = current.manualDecision(command.decision(), command.reason(), clock);
        return claimRepository.save(decided);
    }
}
