package com.elgourmat.careflow.application.port.in;

import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;

import java.util.UUID;

public interface DecideClaimUseCase {

    Claim decide(DecideClaimCommand command);

    record DecideClaimCommand(UUID claimId, ClaimStatus decision, String reason) {
    }
}
