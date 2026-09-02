package com.elgourmat.careflow.adapter.in.rest;

import com.elgourmat.careflow.adapter.in.rest.error.GlobalExceptionHandler;
import com.elgourmat.careflow.adapter.in.rest.mapper.ClaimRestMapperImpl;
import com.elgourmat.careflow.adapter.out.persistence.IdempotencyKeyStore;
import com.elgourmat.careflow.application.port.in.DecideClaimUseCase;
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

    @MockitoBean
    private DecideClaimUseCase decideClaimUseCase;

    @MockitoBean
    private IdempotencyKeyStore idempotencyKeys;

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

    @Test
    void POST_without_idempotency_key_bypasses_store() throws Exception {
        Claim decided = decidedClaim(ClaimStatus.APPROVED, "auto", new BigDecimal("50"));
        given(submitClaimUseCase.submit(any(SubmitClaimCommand.class))).willReturn(decided);

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson("50.00")))
                .andExpect(status().isCreated());

        org.mockito.Mockito.verify(idempotencyKeys, org.mockito.Mockito.never()).lookup(org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.verify(idempotencyKeys, org.mockito.Mockito.never()).store(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(UUID.class));
    }

    @Test
    void POST_with_new_idempotency_key_submits_and_stores() throws Exception {
        Claim decided = decidedClaim(ClaimStatus.APPROVED, "auto", new BigDecimal("50"));
        given(idempotencyKeys.lookup("ik_first")).willReturn(Optional.empty());
        given(submitClaimUseCase.submit(any(SubmitClaimCommand.class))).willReturn(decided);

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "ik_first")
                        .content(payloadJson("50.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(decided.id().toString()));

        org.mockito.Mockito.verify(idempotencyKeys).store("ik_first", decided.id());
    }

    @Test
    void POST_with_replayed_idempotency_key_returns_cached_claim_without_resubmitting() throws Exception {
        Claim cached = decidedClaim(ClaimStatus.APPROVED, "auto", new BigDecimal("50"));
        given(idempotencyKeys.lookup("ik_replay")).willReturn(Optional.of(cached.id()));
        given(getClaimUseCase.getById(cached.id())).willReturn(cached);

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "ik_replay")
                        .content(payloadJson("50.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cached.id().toString()));

        org.mockito.Mockito.verify(submitClaimUseCase, org.mockito.Mockito.never()).submit(any(SubmitClaimCommand.class));
        org.mockito.Mockito.verify(idempotencyKeys, org.mockito.Mockito.never()).store(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(UUID.class));
    }

    @Test
    void PATCH_decision_returns_200_and_decides_claim() throws Exception {
        Claim decided = decidedClaim(ClaimStatus.APPROVED, "reçu OK", new BigDecimal("300"));
        given(decideClaimUseCase.decide(any(com.elgourmat.careflow.application.port.in.DecideClaimUseCase.DecideClaimCommand.class)))
                .willReturn(decided);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/claims/{id}/decision", decided.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "admin-01")
                        .content("""
                                { "decision": "APPROVED", "reason": "reçu OK" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decisionReason").value("reçu OK"));
    }

    @Test
    void PATCH_decision_with_blank_reason_returns_400() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/claims/{id}/decision", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decision": "APPROVED", "reason": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("reason"));
    }

    @Test
    void PATCH_decision_on_already_decided_claim_returns_409_problem_detail() throws Exception {
        UUID id = UUID.randomUUID();
        given(decideClaimUseCase.decide(any(com.elgourmat.careflow.application.port.in.DecideClaimUseCase.DecideClaimCommand.class)))
                .willThrow(new com.elgourmat.careflow.domain.exception.IllegalClaimStateException(
                        id, ClaimStatus.APPROVED, "Claim " + id + " is already APPROVED and cannot be re-decided"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/claims/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decision": "REJECTED", "reason": "trying to override" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Illegal claim state"))
                .andExpect(jsonPath("$.currentStatus").value("APPROVED"));
    }

    private static String payloadJson(String amount) {
        return """
                {
                  "patientId": "patient-42",
                  "careType": "DENTAL",
                  "amount": %s,
                  "currency": "EUR",
                  "careDate": "2026-08-15"
                }
                """.formatted(amount);
    }

    private static Claim decidedClaim(ClaimStatus status, String reason, BigDecimal amount) {
        Claim submitted = Claim.newSubmission("patient-42", CareType.DENTAL,
                new Money(amount, "EUR"), LocalDate.of(2026, 8, 15), CLOCK);
        return submitted.decide(status, reason, CLOCK);
    }
}
