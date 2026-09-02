package com.elgourmat.careflow.adapter.out.persistence.entite;

import com.elgourmat.careflow.domain.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimJpaRepository extends JpaRepository<ClaimEntity, UUID> {

    List<ClaimEntity> findByStatus(ClaimStatus status);
}
