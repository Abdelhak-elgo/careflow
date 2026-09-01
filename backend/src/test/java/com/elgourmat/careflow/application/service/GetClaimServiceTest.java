package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.Money;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetClaimServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ClaimRepository claimRepository;

    private GetClaimService service;

    @BeforeEach
    void setUp() {
        service = new GetClaimService(claimRepository);
    }

    @Test
    void returns_claim_when_found() {
        Claim claim = Claim.newSubmission("patient-42", CareType.DENTAL,
                new Money(new BigDecimal("10.00"), "EUR"), LocalDate.of(2026, 8, 15), CLOCK);
        when(claimRepository.findById(claim.id())).thenReturn(Optional.of(claim));

        Claim result = service.getById(claim.id());

        assertThat(result).isSameAs(claim);
    }

    @Test
    void throws_ClaimNotFoundException_when_missing() {
        UUID unknown = UUID.randomUUID();
        when(claimRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(unknown))
                .isInstanceOf(ClaimNotFoundException.class)
                .hasMessageContaining(unknown.toString());
    }
}
