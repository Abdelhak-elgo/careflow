package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.SubmitClaimUseCase;
import com.elgourmat.careflow.application.port.out.AuditPort;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.Money;
import com.elgourmat.careflow.domain.rules.ClaimRulesEngine;
import com.elgourmat.careflow.domain.rules.Decision;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SubmitClaimService implements SubmitClaimUseCase {

    private final ClaimRulesEngine rulesEngine;
    private final ClaimRepository claimRepository;
    private final AuditPort auditPort;
    private final Clock clock;

    public SubmitClaimService(ClaimRulesEngine rulesEngine, ClaimRepository claimRepository, AuditPort auditPort, Clock clock) {
        this.rulesEngine = Objects.requireNonNull(rulesEngine);
        this.claimRepository = Objects.requireNonNull(claimRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Claim submit(SubmitClaimCommand command) {
        Objects.requireNonNull(command, "command is required");
        Money money = new Money(command.amount(), command.currency());
        Claim submitted = Claim.newSubmission(
                command.patientId(),
                command.careType(),
                money,
                command.careDate(),
                clock
        );
        Decision decision = rulesEngine.evaluate(submitted);
        Claim decided = submitted.decide(decision.status(), decision.reason(), clock);
        Claim persisted = claimRepository.save(decided);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("status", persisted.status().name());
        details.put("careType", persisted.careType().name());
        details.put("amount", persisted.money().amount().toPlainString());
        details.put("currency", persisted.money().currency());
        if (persisted.decisionReason() != null) {
            details.put("reason", persisted.decisionReason());
        }
        auditPort.record("CLAIM_SUBMITTED", "CLAIM", persisted.id().toString(), details);

        return persisted;
    }
}
