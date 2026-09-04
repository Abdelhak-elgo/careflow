package com.elgourmat.careflow.adapter.in.rest.error;

import com.elgourmat.careflow.adapter.out.storage.StorageException;
import com.elgourmat.careflow.domain.exception.AttachmentNotFoundException;
import com.elgourmat.careflow.domain.exception.ClaimNotFoundException;
import com.elgourmat.careflow.domain.exception.InvalidClaimAmountException;
import com.elgourmat.careflow.domain.exception.InvalidClaimDateException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns_404_with_claim_id_property() {
        UUID id = UUID.randomUUID();

        ProblemDetail pd = handler.handleNotFound(new ClaimNotFoundException(id));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Claim not found");
        assertThat(pd.getDetail()).contains(id.toString());
        assertThat(pd.getProperties()).containsEntry("claimId", id.toString());
    }

    @Test
    void handleInvalidClaim_amount_returns_400() {
        ProblemDetail pd = handler.handleInvalidClaim(new InvalidClaimAmountException("amount must be > 0"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getTitle()).isEqualTo("Invalid claim");
        assertThat(pd.getDetail()).isEqualTo("amount must be > 0");
    }

    @Test
    void handleInvalidClaim_date_returns_400() {
        ProblemDetail pd = handler.handleInvalidClaim(new InvalidClaimDateException("careDate in future"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).isEqualTo("careDate in future");
    }

    @Test
    void handleValidation_returns_400_with_field_violations() throws Exception {
        BindingResult binding = new BeanPropertyBindingResult(new Object(), "submitClaimRequest");
        binding.addError(new FieldError("submitClaimRequest", "amount", "must be greater than zero"));
        binding.addError(new FieldError("submitClaimRequest", "patientId", "must not be blank"));
        Method dummy = DummyController.class.getDeclaredMethod("dummy");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new MethodParameter(dummy, -1), binding);

        ProblemDetail pd = handler.handleValidation(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getTitle()).isEqualTo("Validation failed");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> violations = (List<Map<String, String>>) pd.getProperties().get("violations");
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(m -> m.get("field")).containsExactlyInAnyOrder("amount", "patientId");
    }

    @Test
    void handleIllegalArgument_returns_400() {
        ProblemDetail pd = handler.handleIllegalArgument(new IllegalArgumentException("bad currency"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).isEqualTo("bad currency");
    }

    @Test
    void handleUnexpected_returns_500_without_leaking_details() {
        ProblemDetail pd = handler.handleUnexpected(new RuntimeException("internal boom leaking secrets"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getTitle()).isEqualTo("Internal server error");
        assertThat(pd.getDetail()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void handleAttachmentNotFound_returns_404_with_attachment_id_property() {
        UUID id = UUID.randomUUID();

        ProblemDetail pd = handler.handleAttachmentNotFound(new AttachmentNotFoundException(id));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Attachment not found");
        assertThat(pd.getDetail()).contains(id.toString());
        assertThat(pd.getProperties()).containsEntry("attachmentId", id.toString());
    }

    @Test
    void handleStorageFailure_returns_502_without_leaking_upstream_details() {
        StorageException ex = new StorageException("Failed to upload object attachments/xyz",
                new RuntimeException("minio credentials rejected — leak"));

        ProblemDetail pd = handler.handleStorageFailure(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(pd.getTitle()).isEqualTo("Storage failure");
        assertThat(pd.getDetail()).isEqualTo("Storage backend is unavailable");
        assertThat(pd.getDetail()).doesNotContain("minio credentials rejected");
        assertThat(pd.getDetail()).doesNotContain("attachments/xyz");
    }

    private static final class DummyController {
        @SuppressWarnings("unused")
        void dummy() {
        }
    }
}
