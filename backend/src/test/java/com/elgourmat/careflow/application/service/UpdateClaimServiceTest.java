package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.in.UpdateClaimUseCase.UpdateClaimCommand;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import com.elgourmat.careflow.domain.Money;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;
import com.elgourmat.careflow.domain.exception.IllegalClaimStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

class UpdateClaimServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-02T12:00:00Z"), ZoneOffset.UTC);

    private ClaimRepository repository;
    private UpdateClaimService service;

    @BeforeEach
    void setUp() {
        repository = mock(ClaimRepository.class);
        service = new UpdateClaimService(repository, CLOCK);
    }

    @Test
    void updates_a_pending_claim_patient_and_date() {
        Claim pending = pendingClaim();
        given(repository.findById(pending.id())).willReturn(Optional.of(pending));
        given(repository.save(any(Claim.class))).willAnswer(i -> i.getArgument(0));

        Claim result = service.update(new UpdateClaimCommand(pending.id(), "patient-99", LocalDate.of(2026, 8, 20)));

        assertThat(result.patientId()).isEqualTo("patient-99");
        assertThat(result.careDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(result.status()).isEqualTo(ClaimStatus.PENDING);
    }

    @Test
    void throws_not_found_for_unknown_id() {
        UUID unknown = UUID.randomUUID();
        given(repository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                new UpdateClaimCommand(unknown, "p", LocalDate.of(2026, 8, 15))))
                .isInstanceOf(ClaimNotFoundException.class);
    }

    @Test
    void refuses_to_update_a_non_pending_claim() {
        Claim approved = pendingClaim().decide(ClaimStatus.APPROVED, "auto", CLOCK);
        given(repository.findById(approved.id())).willReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.update(
                new UpdateClaimCommand(approved.id(), "p", LocalDate.of(2026, 8, 15))))
                .isInstanceOf(IllegalClaimStateException.class)
                .hasMessageContaining("can no longer be edited");
    }

    private static Claim pendingClaim() {
        return Claim.newSubmission("patient-42", CareType.DENTAL,
                new Money(new BigDecimal("300"), "EUR"), LocalDate.of(2026, 8, 15), CLOCK);
    }
}
