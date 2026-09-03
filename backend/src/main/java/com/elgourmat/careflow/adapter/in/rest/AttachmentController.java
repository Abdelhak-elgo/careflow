package com.elgourmat.careflow.adapter.in.rest;

import com.elgourmat.careflow.adapter.in.rest.dto.AttachmentResponse;
import com.elgourmat.careflow.adapter.in.rest.dto.RenameAttachmentRequest;
import com.elgourmat.careflow.adapter.in.rest.mapper.AttachmentRestMapper;
import com.elgourmat.careflow.adapter.out.persistence.IdempotencyKeyStore;
import com.elgourmat.careflow.application.port.in.AttachmentUseCase;
import com.elgourmat.careflow.application.port.in.AttachmentUseCase.DownloadResult;
import com.elgourmat.careflow.application.port.in.AttachmentUseCase.RenameAttachmentCommand;
import com.elgourmat.careflow.application.port.in.AttachmentUseCase.UploadAttachmentCommand;
import com.elgourmat.careflow.domain.attachment.ClaimAttachment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@Tag(name = "Attachments", description = "Pièces jointes attachées à une demande")
public class AttachmentController {

    private static final Logger log = LoggerFactory.getLogger(AttachmentController.class);
    private static final String DEFAULT_CONTENT_TYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    private static final String ANONYMOUS = "anonymous";

    private final AttachmentUseCase attachmentUseCase;
    private final AttachmentRestMapper mapper;
    private final IdempotencyKeyStore idempotencyKeys;

    public AttachmentController(AttachmentUseCase attachmentUseCase,
                                AttachmentRestMapper mapper,
                                IdempotencyKeyStore idempotencyKeys) {
        this.attachmentUseCase = attachmentUseCase;
        this.mapper = mapper;
        this.idempotencyKeys = idempotencyKeys;
    }

    @PostMapping(path = "/api/claims/{claimId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Téléverser une pièce jointe pour une demande")
    @ApiResponse(responseCode = "201", description = "Pièce jointe créée")
    @ApiResponse(responseCode = "404", description = "Demande inconnue (RFC 7807)")
    public ResponseEntity<AttachmentResponse> upload(
            @PathVariable UUID claimId,
            @RequestPart("file") MultipartFile file,
            @Parameter(in = ParameterIn.HEADER, name = "X-User-Id")
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key",
                    description = "Clé opaque ≤128 chars. Rejouer la même clé renvoie la pièce jointe initiale (TTL 24h).")
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) throws IOException {
        String uploader = resolveActor(userId);
        String contentType = file.getContentType() == null ? DEFAULT_CONTENT_TYPE : file.getContentType();
        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        log.info("upload attachment claim={} user={} name={} size={} idempotencyKey={}",
                claimId, uploader, originalName, file.getSize(), idempotencyKey);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (idempotencyKey.length() > 128) {
                throw new IllegalArgumentException("Idempotency-Key must not exceed 128 characters");
            }
            Optional<UUID> cached = idempotencyKeys.lookup(idempotencyKey, IdempotencyKeyStore.RESOURCE_ATTACHMENT);
            if (cached.isPresent()) {
                ClaimAttachment existing = attachmentUseCase.getById(cached.get());
                log.info("idempotency hit: returning existing attachment id={}", existing.id());
                return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(existing));
            }
        }

        ClaimAttachment saved = attachmentUseCase.upload(new UploadAttachmentCommand(
                claimId,
                originalName,
                contentType,
                file.getSize(),
                file.getInputStream(),
                uploader
        ));

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyKeys.store(idempotencyKey, IdempotencyKeyStore.RESOURCE_ATTACHMENT, saved.id());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }

    @GetMapping("/api/claims/{claimId}/attachments")
    @Operation(summary = "Lister les pièces jointes d'une demande")
    public List<AttachmentResponse> listByClaim(@PathVariable UUID claimId) {
        return mapper.toResponses(attachmentUseCase.listByClaim(claimId));
    }

    @GetMapping("/api/attachments/{id}")
    @Operation(summary = "Télécharger le contenu binaire d'une pièce jointe")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        DownloadResult result = attachmentUseCase.download(id);
        ClaimAttachment attachment = result.attachment();
        String filename = URLEncoder.encode(attachment.originalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(attachment.contentType()));
        headers.setContentLength(attachment.sizeBytes());
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + filename);
        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(result.stream()));
    }

    @DeleteMapping("/api/attachments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une pièce jointe (admin)")
    @ApiResponse(responseCode = "204", description = "Supprimée")
    @ApiResponse(responseCode = "403", description = "Rôle ADMIN manquant")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        attachmentUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/attachments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Renommer une pièce jointe (admin)")
    public AttachmentResponse rename(
            @PathVariable UUID id,
            @Valid @RequestBody RenameAttachmentRequest request
    ) {
        ClaimAttachment renamed = attachmentUseCase.rename(new RenameAttachmentCommand(id, request.originalName()));
        return mapper.toResponse(renamed);
    }

    @GetMapping("/api/admin/attachments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister toutes les pièces jointes (admin)")
    public List<AttachmentResponse> listAll() {
        return mapper.toResponses(attachmentUseCase.listAll());
    }

    private static String resolveActor(String userIdHeader) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return userIdHeader;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ANONYMOUS;
        }
        return auth.getName();
    }
}
