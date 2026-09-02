package com.elgourmat.careflow.adapter.in.rest;

import com.elgourmat.careflow.adapter.in.rest.error.GlobalExceptionHandler;
import com.elgourmat.careflow.adapter.in.rest.mapper.ClaimRestMapperImpl;
import com.elgourmat.careflow.application.port.in.GetClaimUseCase;
import com.elgourmat.careflow.application.port.in.ListClaimsUseCase;
import com.elgourmat.careflow.application.port.in.SubmitClaimUseCase;
import com.elgourmat.careflow.application.port.in.SubmitClaimUseCase.SubmitClaimCommand;
import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import com.elgourmat.careflow.domain.Money;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
@Import({ClaimRestMapperImpl.class, GlobalExceptionHandler.class})
class ClaimControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmitClaimUseCase submitClaimUseCase;

    @MockitoBean
    private ListClaimsUseCase listClaimsUseCase;

    @MockitoBean
    private GetClaimUseCase getClaimUseCase;

    @Test
    void POST_valid_returns_201_with_response_body() throws Exception {
        Claim decided = decidedClaim(ClaimStatus.APPROVED, "Auto-approval: amount below 100 EUR", new BigDecimal("50"));
        given(submitClaimUseCase.submit(any(SubmitClaimCommand.class))).willReturn(decided);

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "patient-42")
                        .content("""
                                {
                                  "patientId": "patient-42",
                                  "careType": "DENTAL",
                                  "amount": 50.00,
                                  "currency": "EUR",
                                  "careDate": "2026-08-15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(decided.id().toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.decisionReason").value("Auto-approval: amount below 100 EUR"));
    }

    @Test
    void POST_negative_amount_returns_400_problem_detail() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "patient-42",
                                  "careType": "DENTAL",
                                  "amount": -10.00,
                                  "currency": "EUR",
                                  "careDate": "2026-08-15"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.violations", hasSize(1)))
                .andExpect(jsonPath("$.violations[0].field").value("amount"));
    }

    @Test
    void POST_blank_patientId_returns_400_problem_detail() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "",
                                  "careType": "DENTAL",
                                  "amount": 50.00,
                                  "currency": "EUR",
                                  "careDate": "2026-08-15"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("patientId"));
    }

    @Test
    void GET_by_id_returns_200() throws Exception {
        Claim decided = decidedClaim(ClaimStatus.PENDING, "Sent to manual review", new BigDecimal("300"));
        given(getClaimUseCase.getById(decided.id())).willReturn(decided);

        mockMvc.perform(get("/api/claims/{id}", decided.id())
                        .header("X-User-Id", "patient-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(decided.id().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void GET_by_id_unknown_returns_404_problem_detail() throws Exception {
        UUID missing = UUID.randomUUID();
        given(getClaimUseCase.getById(missing)).willThrow(new ClaimNotFoundException(missing));

        mockMvc.perform(get("/api/claims/{id}", missing))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Claim not found"))
                .andExpect(jsonPath("$.claimId").value(missing.toString()));
    }

    @Test
    void GET_list_with_status_filter_returns_200_with_matching_claims() throws Exception {
        Claim pending = decidedClaim(ClaimStatus.PENDING, "Sent to manual review", new BigDecimal("300"));
        given(listClaimsUseCase.list(eq(Optional.of(ClaimStatus.PENDING)))).willReturn(List.of(pending));

        mockMvc.perform(get("/api/claims").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void GET_list_without_filter_returns_all() throws Exception {
        given(listClaimsUseCase.list(Optional.empty())).willReturn(List.of(
                decidedClaim(ClaimStatus.APPROVED, "a", new BigDecimal("50")),
                decidedClaim(ClaimStatus.PENDING, "b", new BigDecimal("300"))
        ));

        mockMvc.perform(get("/api/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    private static Claim decidedClaim(ClaimStatus status, String reason, BigDecimal amount) {
        Claim submitted = Claim.newSubmission("patient-42", CareType.DENTAL,
                new Money(amount, "EUR"), LocalDate.of(2026, 8, 15), CLOCK);
        return submitted.decide(status, reason, CLOCK);
    }
}
