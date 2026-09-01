package com.elgourmat.careflow.application.port.in;

import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.Claim;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SubmitClaimUseCase {

    Claim submit(SubmitClaimCommand command);

    record SubmitClaimCommand(
            String patientId,
            CareType careType,
            BigDecimal amount,
            String currency,
            LocalDate careDate
    ) {
    }
}
