package com.elgourmat.careflow.adapter.in.rest.error;

import com.elgourmat.careflow.adapter.out.storage.StorageException;
import com.elgourmat.careflow.domain.exception.AttachmentNotFoundException;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;
import com.elgourmat.careflow.domain.exception.IllegalClaimStateException;
import com.elgourmat.careflow.domain.exception.InvalidClaimAmountException;
import com.elgourmat.careflow.domain.exception.InvalidClaimDateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()
                ))
                .collect(Collectors.toList());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request payload validation failed");
        pd.setTitle("Validation failed");
        pd.setProperty("violations", violations);
        return pd;
    }

    @ExceptionHandler({InvalidClaimAmountException.class, InvalidClaimDateException.class})
    public ProblemDetail handleInvalidClaim(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid claim");
        return pd;
    }

    @ExceptionHandler(ClaimNotFoundException.class)
    public ProblemDetail handleNotFound(ClaimNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Claim not found");
        pd.setProperty("claimId", ex.claimId().toString());
        return pd;
    }

    @ExceptionHandler(AttachmentNotFoundException.class)
    public ProblemDetail handleAttachmentNotFound(AttachmentNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Attachment not found");
        pd.setProperty("attachmentId", ex.attachmentId().toString());
        return pd;
    }

    @ExceptionHandler(IllegalClaimStateException.class)
    public ProblemDetail handleIllegalState(IllegalClaimStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Illegal claim state");
        pd.setProperty("claimId", ex.claimId().toString());
        pd.setProperty("currentStatus", ex.currentStatus().name());
        return pd;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file exceeds the allowed size limit");
        pd.setTitle("Payload too large");
        return pd;
    }

    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorageFailure(StorageException ex) {
        log.error("Object storage failure", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "Storage backend is unavailable");
        pd.setTitle("Storage failure");
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid request");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        pd.setTitle("Internal server error");
        return pd;
    }
}
