package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.SubmitClaimUseCase.SubmitClaimCommand;
import com.elgourmat.careflow.application.port.out.AuditPort;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import com.elgourmat.careflow.domain.rules.ClaimRulesEngine;
import com.elgourmat.careflow.domain.rules.Decision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitClaimServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-01T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock
    private ClaimRulesEngine rulesEngine;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private AuditPort auditPort;

    private SubmitClaimService service;

    @BeforeEach
    void setUp() {
        service = new SubmitClaimService(rulesEngine, claimRepository, auditPort, FIXED_CLOCK);
    }

    @Test
    void submits_evaluates_and_persists_the_decided_claim() {
        SubmitClaimCommand command = new SubmitClaimCommand(
                "patient-42", CareType.DENTAL, new BigDecimal("50.00"), "EUR", LocalDate.of(2026, 8, 15));
        when(rulesEngine.evaluate(any(Claim.class)))
                .thenReturn(new Decision(ClaimStatus.APPROVED, "auto"));
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim result = service.submit(command);

        ArgumentCaptor<Claim> savedCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(savedCaptor.capture());
        Claim saved = savedCaptor.getValue();

        assertThat(saved.status()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(saved.decisionReason()).isEqualTo("auto");
        assertThat(saved.submittedAt()).isEqualTo(FIXED_NOW);
        assertThat(saved.decidedAt()).isEqualTo(FIXED_NOW);
        assertThat(saved.patientId()).isEqualTo("patient-42");
        assertThat(saved.money().amount()).isEqualByComparingTo("50.00");
        assertThat(saved.money().currency()).isEqualTo("EUR");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void feeds_pending_status_from_engine_to_persisted_claim() {
        SubmitClaimCommand command = new SubmitClaimCommand(
                "patient-42", CareType.GENERAL, new BigDecimal("300"), "EUR", LocalDate.of(2026, 8, 15));
        when(rulesEngine.evaluate(any(Claim.class)))
                .thenReturn(new Decision(ClaimStatus.PENDING, "Sent to manual review"));
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim result = service.submit(command);

        assertThat(result.status()).isEqualTo(ClaimStatus.PENDING);
        assertThat(result.decisionReason()).isEqualTo("Sent to manual review");
    }
}
