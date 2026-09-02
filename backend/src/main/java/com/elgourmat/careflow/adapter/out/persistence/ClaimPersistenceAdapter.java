package com.elgourmat.careflow.adapter.out.persistence;

import com.elgourmat.careflow.adapter.out.persistence.entite.ClaimEntity;
import com.elgourmat.careflow.adapter.out.persistence.entite.ClaimJpaRepository;
import com.elgourmat.careflow.adapter.out.persistence.mapper.ClaimEntityMapper;
import com.elgourmat.careflow.application.port.out.ClaimRepository;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class ClaimPersistenceAdapter implements ClaimRepository {

    private final ClaimJpaRepository claimJpaRepository;
    private final ClaimEntityMapper claimEntityMapper;

    public ClaimPersistenceAdapter(ClaimJpaRepository claimJpaRepository, ClaimEntityMapper claimEntityMapper) {
        this.claimJpaRepository = Objects.requireNonNull(claimJpaRepository);
        this.claimEntityMapper = Objects.requireNonNull(claimEntityMapper);
    }

    @Override
    public Claim save(Claim claim) {
        ClaimEntity entity = claimEntityMapper.toEntity(claim);
        ClaimEntity saved = claimJpaRepository.save(entity);
        return claimEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Claim> findById(UUID id) {
        return claimJpaRepository.findById(id).map(claimEntityMapper::toDomain);
    }

    @Override
    public List<Claim> findByStatus(ClaimStatus status) {
        return claimJpaRepository.findByStatus(status).stream()
                .map(claimEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Claim> findAll() {
        return claimJpaRepository.findAll().stream()
                .map(claimEntityMapper::toDomain)
                .toList();
    }
}
