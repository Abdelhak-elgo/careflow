package com.elgourmat.careflow.application.port.in;

import com.elgourmat.careflow.domain.Claim;

import java.time.LocalDate;
import java.util.UUID;

public interface UpdateClaimUseCase {

    Claim update(UpdateClaimCommand command);

    record UpdateClaimCommand(UUID claimId, String patientId, LocalDate careDate) {
    }
}
