package com.elgourmat.careflow.adapter.out.persistence;

import com.elgourmat.careflow.adapter.out.persistence.mapper.ClaimEntityMapperImpl;
import com.elgourmat.careflow.domain.CareType;
import com.elgourmat.careflow.domain.Claim;
import com.elgourmat.careflow.domain.ClaimStatus;
import com.elgourmat.careflow.domain.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ClaimPersistenceAdapter.class, ClaimEntityMapperImpl.class})
class ClaimPersistenceAdapterTest extends AbstractPostgresIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private ClaimPersistenceAdapter adapter;

    @Test
    void save_and_findById_roundtrip_preserves_all_fields() {
        Claim submitted = Claim.newSubmission("patient-42", CareType.DENTAL,
                new Money(new BigDecimal("89.50"), "EUR"), LocalDate.of(2026, 8, 15), CLOCK);
        Claim decided = submitted.decide(ClaimStatus.APPROVED, "Auto-approval: amount below 100 EUR", CLOCK);

        Claim saved = adapter.save(decided);
        Optional<Claim> loaded = adapter.findById(saved.id());

        assertThat(loaded).isPresent();
        Claim reloaded = loaded.get();
        assertThat(reloaded.id()).isEqualTo(decided.id());
        assertThat(reloaded.patientId()).isEqualTo("patient-42");
        assertThat(reloaded.careType()).isEqualTo(CareType.DENTAL);
        assertThat(reloaded.money().amount()).isEqualByComparingTo("89.50");
        assertThat(reloaded.money().currency()).isEqualTo("EUR");
        assertThat(reloaded.careDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(reloaded.status()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(reloaded.decisionReason()).isEqualTo("Auto-approval: amount below 100 EUR");
        assertThat(reloaded.submittedAt()).isEqualTo(decided.submittedAt());
        assertThat(reloaded.decidedAt()).isEqualTo(decided.decidedAt());
    }

    @Test
    void findById_returns_empty_when_missing() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findByStatus_returns_only_matching_claims() {
        adapter.save(claim(ClaimStatus.PENDING, new BigDecimal("300")));
        adapter.save(claim(ClaimStatus.PENDING, new BigDecimal("400")));
        adapter.save(claim(ClaimStatus.APPROVED, new BigDecimal("50")));

        List<Claim> pending = adapter.findByStatus(ClaimStatus.PENDING);
        List<Claim> approved = adapter.findByStatus(ClaimStatus.APPROVED);

        assertThat(pending).hasSize(2).allMatch(c -> c.status() == ClaimStatus.PENDING);
        assertThat(approved).hasSize(1).allMatch(c -> c.status() == ClaimStatus.APPROVED);
    }

    @Test
    void findAll_returns_every_row() {
        int before = adapter.findAll().size();
        adapter.save(claim(ClaimStatus.PENDING, new BigDecimal("10")));
        adapter.save(claim(ClaimStatus.PENDING, new BigDecimal("20")));

        assertThat(adapter.findAll()).hasSize(before + 2);
    }

    private static Claim claim(ClaimStatus status, BigDecimal amount) {
        Claim submitted = Claim.newSubmission("patient-42", CareType.DENTAL,
                new Money(amount, "EUR"), LocalDate.of(2026, 8, 15), CLOCK);
        return submitted.decide(status, "seed", CLOCK);
    }
}
