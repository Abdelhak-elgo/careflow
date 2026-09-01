package com.elgourmat.careflow.application.port.out;

import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository {

    Claim save(Claim claim);

    Optional<Claim> findById(UUID id);

    List<Claim> findByStatus(ClaimStatus status);

    List<Claim> findAll();
}
