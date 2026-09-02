package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.UpdateClaimUseCase;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;

import java.time.Clock;
import java.util.Objects;

public class UpdateClaimService implements UpdateClaimUseCase {

    private final ClaimRepository claimRepository;
    private final Clock clock;

    public UpdateClaimService(ClaimRepository claimRepository, Clock clock) {
        this.claimRepository = Objects.requireNonNull(claimRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Claim update(UpdateClaimCommand command) {
        Objects.requireNonNull(command, "command is required");
        Claim current = claimRepository.findById(command.claimId())
                .orElseThrow(() -> new ClaimNotFoundException(command.claimId()));
        Claim updated = current.updatePatientInfo(command.patientId(), command.careDate(), clock);
        return claimRepository.save(updated);
    }
}
