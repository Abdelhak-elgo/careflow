package com.elgourmat.careflow.application.port.in;

import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;

import java.util.List;
import java.util.Optional;

public interface ListClaimsUseCase {

    List<Claim> list(Optional<ClaimStatus> statusFilter);
}
