package com.elgourmat.careflow.application.port.in;

import com.elgourmat.careflow.domain.Claim;

import java.util.UUID;

public interface GetClaimUseCase {

    Claim getById(UUID id);
}
