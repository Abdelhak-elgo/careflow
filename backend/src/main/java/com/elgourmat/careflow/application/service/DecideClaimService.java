package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.DecideClaimUseCase;
import com.elgourmat.careflow.application.port.out.AuditPort;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class DecideClaimService implements DecideClaimUseCase {

    private final ClaimRepository claimRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public DecideClaimService(ClaimRepository claimRepository, AuditPort auditPort, Clock clock) {
        this.claimRepository = Objects.requireNonNull(claimRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Claim decide(DecideClaimCommand command) {
        Objects.requireNonNull(command, "command is required");
        Claim current = claimRepository.findById(command.claimId())
                .orElseThrow(() -> new ClaimNotFoundException(command.claimId()));
        Claim decided = current.manualDecision(command.decision(), command.reason(), clock);
        Claim persisted = claimRepository.save(decided);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("oldStatus", current.status().name());
        details.put("newStatus", persisted.status().name());
        details.put("reason", command.reason());
        auditPort.record("CLAIM_DECIDED", "CLAIM", persisted.id().toString(), details);

        return persisted;
    }
}
