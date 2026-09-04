package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.DecideClaimUseCase.DecideClaimCommand;
import com.elgourmat.careflow.application.port.out.AuditPort;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import com.elgourmat.careflow.domain.Money;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;
import com.elgourmat.careflow.domain.exception.IllegalClaimStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DecideClaimServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-02T11:30:00Z"), ZoneOffset.UTC);

    private ClaimRepository repository;
    private DecideClaimService service;

    @BeforeEach
    void setUp() {
        repository = mock(ClaimRepository.class);
        service = new DecideClaimService(repository, mock(AuditPort.class), CLOCK);
    }

    @Test
    void decides_a_pending_claim_and_persists_the_updated_state() {
        Claim pending = pendingClaim();
        given(repository.findById(pending.id())).willReturn(Optional.of(pending));
        given(repository.save(any(Claim.class))).willAnswer(i -> i.getArgument(0));

        Claim result = service.decide(new DecideClaimCommand(pending.id(), ClaimStatus.APPROVED, "reçu fourni"));

        assertThat(result.status()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(result.decisionReason()).isEqualTo("reçu fourni");
        assertThat(result.decidedAt()).isEqualTo(Instant.parse("2026-09-02T11:30:00Z"));

        ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(pending.id());
    }

    @Test
    void throws_not_found_when_claim_is_unknown() {
        UUID unknown = UUID.randomUUID();
        given(repository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.decide(
                new DecideClaimCommand(unknown, ClaimStatus.APPROVED, "any")))
                .isInstanceOf(ClaimNotFoundException.class);
    }

    @Test
    void refuses_to_decide_a_claim_already_approved() {
        Claim approved = pendingClaim().decide(ClaimStatus.APPROVED, "auto", CLOCK);
        given(repository.findById(approved.id())).willReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.decide(
                new DecideClaimCommand(approved.id(), ClaimStatus.REJECTED, "trying to override")))
                .isInstanceOf(IllegalClaimStateException.class)
                .hasMessageContaining("cannot be re-decided");
    }

    @Test
    void refuses_to_decide_towards_pending() {
        Claim pending = pendingClaim();
        given(repository.findById(pending.id())).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.decide(
                new DecideClaimCommand(pending.id(), ClaimStatus.PENDING, "no-op")))
                .isInstanceOf(IllegalClaimStateException.class)
                .hasMessageContaining("APPROVED or REJECTED");
    }

    private static Claim pendingClaim() {
        return Claim.newSubmission("patient-42", CareType.DENTAL,
                new Money(new BigDecimal("300"), "EUR"), LocalDate.of(2026, 8, 15), CLOCK);
    }
}
