package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.UpdateClaimUseCase;
import com.elgourmat.careflow.application.port.out.AuditPort;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class UpdateClaimService implements UpdateClaimUseCase {

    private final ClaimRepository claimRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public UpdateClaimService(ClaimRepository claimRepository, AuditPort auditPort, Clock clock) {
        this.claimRepository = Objects.requireNonNull(claimRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Claim update(UpdateClaimCommand command) {
        Objects.requireNonNull(command, "command is required");
        Claim current = claimRepository.findById(command.claimId())
                .orElseThrow(() -> new ClaimNotFoundException(command.claimId()));
        Claim updated = current.updatePatientInfo(command.patientId(), command.careDate(), clock);
        Claim persisted = claimRepository.save(updated);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("oldPatientId", current.patientId());
        details.put("newPatientId", persisted.patientId());
        details.put("oldCareDate", current.careDate().toString());
        details.put("newCareDate", persisted.careDate().toString());
        auditPort.record("CLAIM_UPDATED", "CLAIM", persisted.id().toString(), details);

        return persisted;
    }
}
