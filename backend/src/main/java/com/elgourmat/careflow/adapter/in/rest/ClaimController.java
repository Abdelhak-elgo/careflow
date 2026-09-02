package com.elgourmat.careflow.adapter.in.rest;

import com.elgourmat.careflow.adapter.in.rest.dto.ClaimResponse;
import com.elgourmat.careflow.adapter.in.rest.dto.SubmitClaimRequest;
import com.elgourmat.careflow.adapter.in.rest.mapper.ClaimRestMapper;
import com.elgourmat.careflow.application.port.in.GetClaimUseCase;
import com.elgourmat.careflow.application.port.in.ListClaimsUseCase;
import com.elgourmat.careflow.application.port.in.SubmitClaimUseCase;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
public class ClaimController {

    private static final Logger log = LoggerFactory.getLogger(ClaimController.class);

    private final SubmitClaimUseCase submitClaimUseCase;
    private final ListClaimsUseCase listClaimsUseCase;
    private final GetClaimUseCase getClaimUseCase;
    private final ClaimRestMapper mapper;

    public ClaimController(
            SubmitClaimUseCase submitClaimUseCase,
            ListClaimsUseCase listClaimsUseCase,
            GetClaimUseCase getClaimUseCase,
            ClaimRestMapper mapper
    ) {
        this.submitClaimUseCase = submitClaimUseCase;
        this.listClaimsUseCase = listClaimsUseCase;
        this.getClaimUseCase = getClaimUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ClaimResponse> submit(
            @Valid @RequestBody SubmitClaimRequest request,
            @RequestHeader(name = "X-User-Id", required = false) String userId
    ) {
        log.info("submit claim by user={} patient={}", userId, request.patientId());
        Claim claim = submitClaimUseCase.submit(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(claim));
    }

    @GetMapping
    public List<ClaimResponse> list(
            @RequestParam(name = "status", required = false) ClaimStatus status,
            @RequestHeader(name = "X-User-Id", required = false) String userId
    ) {
        log.info("list claims by user={} status={}", userId, status);
        List<Claim> claims = listClaimsUseCase.list(Optional.ofNullable(status));
        return mapper.toResponses(claims);
    }

    @GetMapping("/{id}")
    public ClaimResponse getById(
            @PathVariable UUID id,
            @RequestHeader(name = "X-User-Id", required = false) String userId
    ) {
        log.info("get claim by user={} id={}", userId, id);
        return mapper.toResponse(getClaimUseCase.getById(id));
    }
}
