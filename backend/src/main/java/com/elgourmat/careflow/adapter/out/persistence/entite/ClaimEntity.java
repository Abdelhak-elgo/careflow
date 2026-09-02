package com.elgourmat.careflow.adapter.out.persistence.entite;

import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.ClaimStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "claim")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEntity {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false, length = 64)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "care_type", nullable = false, length = 32)
    private CareType careType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "care_date", nullable = false)
    private LocalDate careDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ClaimStatus status;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;
}
