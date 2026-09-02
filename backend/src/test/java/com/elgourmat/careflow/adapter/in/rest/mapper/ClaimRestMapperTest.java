package com.elgourmat.careflow.adapter.in.rest.mapper;

import com.elgourmat.careflow.adapter.in.rest.dto.ClaimResponse;
import com.elgourmat.careflow.adapter.in.rest.dto.SubmitClaimRequest;
import com.elgourmat.careflow.application.port.in.SubmitClaimUseCase.SubmitClaimCommand;
import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import com.elgourmat.careflow.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimRestMapperTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);

    private final ClaimRestMapper mapper = new ClaimRestMapperImpl();

    @Test
    void toCommand_copies_all_request_fields() {
        SubmitClaimRequest request = new SubmitClaimRequest(
                "patient-42", CareType.DENTAL, new BigDecimal("89.50"), "EUR", LocalDate.of(2026, 8, 15)
        );

        SubmitClaimCommand command = mapper.toCommand(request);

        assertThat(command.patientId()).isEqualTo("patient-42");
        assertThat(command.careType()).isEqualTo(CareType.DENTAL);
        assertThat(command.amount()).isEqualByComparingTo("89.50");
        assertThat(command.currency()).isEqualTo("EUR");
        assertThat(command.careDate()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void toResponse_flattens_money_into_amount_and_currency() {
        Claim submitted = Claim.newSubmission("patient-42", CareType.DENTAL,
                new Money(new BigDecimal("89.50"), "EUR"), LocalDate.of(2026, 8, 15), CLOCK);
        Claim decided = submitted.decide(ClaimStatus.APPROVED, "auto", CLOCK);

        ClaimResponse response = mapper.toResponse(decided);

        assertThat(response.id()).isEqualTo(decided.id());
        assertThat(response.patientId()).isEqualTo("patient-42");
        assertThat(response.careType()).isEqualTo(CareType.DENTAL);
        assertThat(response.amount()).isEqualByComparingTo("89.50");
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.careDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(response.status()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(response.decisionReason()).isEqualTo("auto");
        assertThat(response.submittedAt()).isEqualTo(decided.submittedAt());
        assertThat(response.decidedAt()).isEqualTo(decided.decidedAt());
    }

    @Test
    void toResponses_maps_every_claim() {
        Claim a = Claim.newSubmission("p1", CareType.DENTAL,
                new Money(new BigDecimal("50"), "EUR"), LocalDate.of(2026, 8, 15), CLOCK);
        Claim b = Claim.newSubmission("p2", CareType.OPTICAL,
                new Money(new BigDecimal("600"), "EUR"), LocalDate.of(2026, 8, 15), CLOCK);

        List<ClaimResponse> responses = mapper.toResponses(List.of(a, b));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).patientId()).isEqualTo("p1");
        assertThat(responses.get(1).patientId()).isEqualTo("p2");
    }
}
