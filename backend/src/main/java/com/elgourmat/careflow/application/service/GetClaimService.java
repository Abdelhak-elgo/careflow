package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.GetClaimUseCase;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;

import java.util.Objects;
import java.util.UUID;

public class GetClaimService implements GetClaimUseCase {

    private final ClaimRepository claimRepository;

    public GetClaimService(ClaimRepository claimRepository) {
        this.claimRepository = Objects.requireNonNull(claimRepository);
    }

    @Override
    public Claim getById(UUID id) {
        Objects.requireNonNull(id, "id is required");
        return claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));
    }
}
