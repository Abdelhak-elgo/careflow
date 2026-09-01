package com.elgourmat.careflow.application.service;

import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import com.elgourmat.careflow.domain.Money;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListClaimsServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ClaimRepository claimRepository;

    private ListClaimsService service;

    @BeforeEach
    void setUp() {
        service = new ListClaimsService(claimRepository);
    }

    @Test
    void delegates_to_findAll_when_no_filter() {
        Claim sample = sampleClaim();
        when(claimRepository.findAll()).thenReturn(List.of(sample));

        List<Claim> result = service.list(Optional.empty());

        assertThat(result).containsExactly(sample);
        verify(claimRepository).findAll();
        verifyNoMoreInteractions(claimRepository);
    }

    @Test
    void delegates_to_findByStatus_when_filter_present() {
        Claim sample = sampleClaim();
        when(claimRepository.findByStatus(ClaimStatus.PENDING)).thenReturn(List.of(sample));

        List<Claim> result = service.list(Optional.of(ClaimStatus.PENDING));

        assertThat(result).containsExactly(sample);
        verify(claimRepository).findByStatus(ClaimStatus.PENDING);
        verifyNoMoreInteractions(claimRepository);
    }

    private static Claim sampleClaim() {
        return Claim.newSubmission("patient-42", CareType.DENTAL,
                new Money(new BigDecimal("10.00"), "EUR"), LocalDate.of(2026, 8, 15), CLOCK);
    }
}
