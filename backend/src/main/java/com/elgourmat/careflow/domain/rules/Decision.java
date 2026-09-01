package com.elgourmat.careflow.domain.rules;

import com.elgourmat.careflow.domain.ClaimStatus;

import java.util.Objects;

public record Decision(ClaimStatus status, String reason) {

    public Decision {
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(reason, "reason is required");
    }
}
