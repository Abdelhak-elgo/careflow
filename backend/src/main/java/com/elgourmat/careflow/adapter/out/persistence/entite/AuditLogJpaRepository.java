package com.elgourmat.careflow.adapter.out.persistence.entite;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {

    @Query("""
            select a from AuditLogEntity a
            where (:entityType is null or a.entityType = :entityType)
              and (:entityId   is null or a.entityId   = :entityId)
              and (:actor      is null or a.actor      = :actor)
            order by a.occurredAt desc
            """)
    Page<AuditLogEntity> search(
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("actor") String actor,
            Pageable pageable
    );
}
