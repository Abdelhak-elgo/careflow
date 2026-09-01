package com.elgourmat.careflow.config;

import com.elgourmat.careflow.domain.rules.ClaimRulesEngine;
import com.elgourmat.careflow.domain.rules.DefaultPendingRule;
import com.elgourmat.careflow.domain.rules.ExpensiveOpticalRule;
import com.elgourmat.careflow.domain.rules.SmallAmountRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class DomainConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ClaimRulesEngine claimRulesEngine() {
        return new ClaimRulesEngine(List.of(
                new SmallAmountRule(),
                new ExpensiveOpticalRule(),
                new DefaultPendingRule()
        ));
    }
}
