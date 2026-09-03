package com.elgourmat.careflow.config;

import com.elgourmat.careflow.application.port.in.AttachmentUseCase;
import com.elgourmat.careflow.application.port.in.DecideClaimUseCase;
import com.elgourmat.careflow.application.port.in.GetClaimUseCase;
import com.elgourmat.careflow.application.port.in.ListClaimsUseCase;
import com.elgourmat.careflow.application.port.in.SubmitClaimUseCase;
import com.elgourmat.careflow.application.port.in.UpdateClaimUseCase;
import com.elgourmat.careflow.application.port.out.AttachmentRepository;
import com.elgourmat.careflow.application.port.out.AuditPort;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.application.port.out.FileStoragePort;
import com.elgourmat.careflow.application.service.AttachmentService;
import com.elgourmat.careflow.application.service.DecideClaimService;
import com.elgourmat.careflow.application.service.GetClaimService;
import com.elgourmat.careflow.application.service.ListClaimsService;
import com.elgourmat.careflow.application.service.SubmitClaimService;
import com.elgourmat.careflow.application.service.UpdateClaimService;
import com.elgourmat.careflow.domain.rules.ClaimRulesEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    SubmitClaimUseCase submitClaimUseCase(
            ClaimRulesEngine rulesEngine,
            ClaimRepository claimRepository,
            AuditPort auditPort,
            Clock clock
    ) {
        return new SubmitClaimService(rulesEngine, claimRepository, auditPort, clock);
    }

    @Bean
    ListClaimsUseCase listClaimsUseCase(ClaimRepository claimRepository) {
        return new ListClaimsService(claimRepository);
    }

    @Bean
    GetClaimUseCase getClaimUseCase(ClaimRepository claimRepository) {
        return new GetClaimService(claimRepository);
    }

    @Bean
    DecideClaimUseCase decideClaimUseCase(ClaimRepository claimRepository, AuditPort auditPort, Clock clock) {
        return new DecideClaimService(claimRepository, auditPort, clock);
    }

    @Bean
    UpdateClaimUseCase updateClaimUseCase(ClaimRepository claimRepository, AuditPort auditPort, Clock clock) {
        return new UpdateClaimService(claimRepository, auditPort, clock);
    }

    @Bean
    AttachmentUseCase attachmentUseCase(
            AttachmentRepository attachmentRepository,
            ClaimRepository claimRepository,
            FileStoragePort fileStoragePort,
            AuditPort auditPort,
            Clock clock
    ) {
        return new AttachmentService(attachmentRepository, claimRepository, fileStoragePort, auditPort, clock);
    }
}
