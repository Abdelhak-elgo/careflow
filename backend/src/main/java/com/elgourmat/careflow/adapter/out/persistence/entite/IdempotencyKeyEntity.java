package com.elgourmat.careflow.adapter.out.persistence.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_key")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "key", length = 128)
    private String key;

    @Column(name = "claim_id", nullable = false)
    private UUID claimId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
