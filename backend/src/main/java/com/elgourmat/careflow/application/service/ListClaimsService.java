package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.ListClaimsUseCase;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ListClaimsService implements ListClaimsUseCase {

    private final ClaimRepository claimRepository;

    public ListClaimsService(ClaimRepository claimRepository) {
        this.claimRepository = Objects.requireNonNull(claimRepository);
    }

    @Override
    public List<Claim> list(Optional<ClaimStatus> statusFilter) {
        Objects.requireNonNull(statusFilter, "statusFilter is required (use Optional.empty() for no filter)");
        return statusFilter
                .map(claimRepository::findByStatus)
                .orElseGet(claimRepository::findAll);
    }
}
