package com.elgourmat.careflow.adapter.in.rest;

import com.elgourmat.careflow.adapter.in.rest.dto.AdminDecisionRequest;
import com.elgourmat.careflow.adapter.in.rest.dto.ClaimResponse;
import com.elgourmat.careflow.adapter.in.rest.dto.SubmitClaimRequest;
import com.elgourmat.careflow.adapter.in.rest.mapper.ClaimRestMapper;
import com.elgourmat.careflow.adapter.out.persistence.IdempotencyKeyStore;
import com.elgourmat.careflow.application.port.in.DecideClaimUseCase;
import com.elgourmat.careflow.application.port.in.GetClaimUseCase;
import com.elgourmat.careflow.application.port.in.ListClaimsUseCase;
import com.elgourmat.careflow.application.port.in.SubmitClaimUseCase;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/claims")
@Tag(name = "Claims", description = "Soumission et consultation des demandes de remboursement")
public class ClaimController {

    private static final Logger log = LoggerFactory.getLogger(ClaimController.class);

    private final SubmitClaimUseCase submitClaimUseCase;
    private final ListClaimsUseCase listClaimsUseCase;
    private final GetClaimUseCase getClaimUseCase;
    private final DecideClaimUseCase decideClaimUseCase;
    private final ClaimRestMapper mapper;
    private final IdempotencyKeyStore idempotencyKeys;

    public ClaimController(
            SubmitClaimUseCase submitClaimUseCase,
            ListClaimsUseCase listClaimsUseCase,
            GetClaimUseCase getClaimUseCase,
            DecideClaimUseCase decideClaimUseCase,
            ClaimRestMapper mapper,
            IdempotencyKeyStore idempotencyKeys
    ) {
        this.submitClaimUseCase = submitClaimUseCase;
        this.listClaimsUseCase = listClaimsUseCase;
        this.getClaimUseCase = getClaimUseCase;
        this.decideClaimUseCase = decideClaimUseCase;
        this.mapper = mapper;
        this.idempotencyKeys = idempotencyKeys;
    }

    @PostMapping
    @Operation(
            summary = "Soumettre une nouvelle demande de remboursement",
            description = "Le moteur de règles décide immédiatement du statut (APPROVED / REJECTED / PENDING). "
                    + "Si l'en-tête Idempotency-Key est fourni et déjà vu, la demande précédente est renvoyée à l'identique."
    )
    @ApiResponse(responseCode = "201", description = "Demande créée avec décision immédiate")
    @ApiResponse(responseCode = "400", description = "Payload invalide (RFC 7807 ProblemDetail)")
    public ResponseEntity<ClaimResponse> submit(
            @Valid @RequestBody SubmitClaimRequest request,
            @Parameter(in = ParameterIn.HEADER, name = "X-User-Id", description = "Identité simulée (MVP)")
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key",
                    description = "Clé opaque ≤128 chars. Rejouer la même clé renvoie la demande initiale (TTL 24h).")
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        log.info("submit claim by user={} patient={} idempotencyKey={}", userId, request.patientId(), idempotencyKey);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (idempotencyKey.length() > 128) {
                throw new IllegalArgumentException("Idempotency-Key must not exceed 128 characters");
            }
            Optional<UUID> cached = idempotencyKeys.lookup(idempotencyKey);
            if (cached.isPresent()) {
                Claim existing = getClaimUseCase.getById(cached.get());
                log.info("idempotency hit: returning existing claim id={}", existing.id());
                return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(existing));
            }
        }

        Claim claim = submitClaimUseCase.submit(mapper.toCommand(request));

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyKeys.store(idempotencyKey, claim.id());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(claim));
    }

    @GetMapping
    @Operation(
            summary = "Lister les demandes",
            description = "Retourne toutes les demandes ou un sous-ensemble filtré par statut."
    )
    @ApiResponse(responseCode = "200", description = "Liste (potentiellement vide) des demandes")
    public List<ClaimResponse> list(
            @Parameter(description = "Filtre optionnel: APPROVED | REJECTED | PENDING")
            @RequestParam(name = "status", required = false) ClaimStatus status,
            @Parameter(in = ParameterIn.HEADER, name = "X-User-Id")
            @RequestHeader(name = "X-User-Id", required = false) String userId
    ) {
        log.info("list claims by user={} status={}", userId, status);
        List<Claim> claims = listClaimsUseCase.list(Optional.ofNullable(status));
        return mapper.toResponses(claims);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une demande par identifiant")
    @ApiResponse(responseCode = "200", description = "Demande trouvée")
    @ApiResponse(responseCode = "404", description = "Demande inconnue (RFC 7807 ProblemDetail)")
    public ClaimResponse getById(
            @Parameter(description = "Identifiant UUID de la demande") @PathVariable UUID id,
            @Parameter(in = ParameterIn.HEADER, name = "X-User-Id")
            @RequestHeader(name = "X-User-Id", required = false) String userId
    ) {
        log.info("get claim by user={} id={}", userId, id);
        return mapper.toResponse(getClaimUseCase.getById(id));
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Trancher manuellement une demande PENDING",
            description = "Route admin (rôle ADMIN requis). Le domaine impose que la demande soit encore PENDING et que la cible soit APPROVED ou REJECTED."
    )
    @ApiResponse(responseCode = "200", description = "Demande décidée manuellement")
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN manquant")
    @ApiResponse(responseCode = "404", description = "Demande inconnue (RFC 7807 ProblemDetail)")
    @ApiResponse(responseCode = "409", description = "Demande déjà tranchée ou cible invalide (RFC 7807 ProblemDetail)")
    public ClaimResponse decide(
            @Parameter(description = "Identifiant UUID de la demande") @PathVariable UUID id,
            @Valid @RequestBody AdminDecisionRequest request,
            @Parameter(in = ParameterIn.HEADER, name = "X-User-Id", description = "Admin identifié (MVP)")
            @RequestHeader(name = "X-User-Id", required = false) String userId
    ) {
        log.info("decide claim by admin={} id={} decision={}", userId, id, request.decision());
        Claim decided = decideClaimUseCase.decide(mapper.toDecideCommand(id, request));
        return mapper.toResponse(decided);
    }
}
