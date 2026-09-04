package com.elgourmat.careflow.domain.exception;

import com.elgourmat.careflow.domain.ClaimStatus;

import java.util.UUID;

public class IllegalClaimStateException extends RuntimeException {

    private final UUID claimId;
    private final ClaimStatus currentStatus;

    public IllegalClaimStateException(UUID claimId, ClaimStatus currentStatus, String message) {
        super(message);
        this.claimId = claimId;
        this.currentStatus = currentStatus;
    }

    public UUID claimId() {
        return claimId;
    }

    public ClaimStatus currentStatus() {
        return currentStatus;
    }
}
