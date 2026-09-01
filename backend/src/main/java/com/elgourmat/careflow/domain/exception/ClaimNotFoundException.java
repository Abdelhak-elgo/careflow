package com.elgourmat.careflow.domain.exception;

import java.util.UUID;

public class ClaimNotFoundException extends RuntimeException {

    private final UUID claimId;

    public ClaimNotFoundException(UUID claimId) {
        super("Claim not found: " + claimId);
        this.claimId = claimId;
    }

    public UUID claimId() {
        return claimId;
    }
}
