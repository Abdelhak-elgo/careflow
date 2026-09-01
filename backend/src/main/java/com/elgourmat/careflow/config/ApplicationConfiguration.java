package com.elgourmat.careflow.config;

import com.elgourmat.careflow.application.port.in.GetClaimUseCase;
import com.elgourmat.careflow.application.port.in.ListClaimsUseCase;
import com.elgourmat.careflow.application.port.in.SubmitClaimUseCase;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.application.service.GetClaimService;
import com.elgourmat.careflow.application.service.ListClaimsService;
import com.elgourmat.careflow.application.service.SubmitClaimService;
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
            Clock clock
    ) {
        return new SubmitClaimService(rulesEngine, claimRepository, clock);
    }

    @Bean
    ListClaimsUseCase listClaimsUseCase(ClaimRepository claimRepository) {
        return new ListClaimsService(claimRepository);
    }

    @Bean
    GetClaimUseCase getClaimUseCase(ClaimRepository claimRepository) {
        return new GetClaimService(claimRepository);
    }
}
