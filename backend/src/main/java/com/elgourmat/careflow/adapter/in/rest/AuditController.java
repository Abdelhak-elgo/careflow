package com.elgourmat.careflow.adapter.in.rest;

import com.elgourmat.careflow.adapter.in.rest.dto.AuditResponse;
import com.elgourmat.careflow.adapter.out.persistence.AuditLogAdapter;
import com.elgourmat.careflow.adapter.out.persistence.entite.AuditLogEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
@Tag(name = "Audit", description = "Journal des actions utilisateur (admin uniquement)")
public class AuditController {

    private final AuditLogAdapter auditLog;

    public AuditController(AuditLogAdapter auditLog) {
        this.auditLog = auditLog;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les événements d'audit",
            description = "Filtres optionnels : entityType, entityId, actor. Pagination via page/size.")
    @ApiResponse(responseCode = "200", description = "Page d'événements (X-Total-Count dans les headers)")
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN manquant")
    public ResponseEntity<List<AuditResponse>> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String actor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int cappedSize = Math.min(Math.max(size, 1), 200);
        Page<AuditLogEntity> results = auditLog.search(
                entityType, entityId, actor, PageRequest.of(Math.max(page, 0), cappedSize));
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(results.getTotalElements()));
        List<AuditResponse> body = results.getContent().stream().map(AuditController::toResponse).toList();
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private static AuditResponse toResponse(AuditLogEntity e) {
        return new AuditResponse(e.getId(), e.getOccurredAt(), e.getActor(), e.getAction(),
                e.getEntityType(), e.getEntityId(), e.getDetails());
    }
}
