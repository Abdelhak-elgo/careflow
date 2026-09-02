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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({
        ClaimPersistenceAdapter.class,
        ClaimEntityMapperImpl.class,
        IdempotencyKeyStore.class,
        IdempotencyKeyStoreTest.FixedClockConfig.class
})
class IdempotencyKeyStoreTest extends AbstractPostgresIntegrationTest {

    static final Instant NOW = Instant.parse("2026-09-02T10:30:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Autowired
    private ClaimPersistenceAdapter claims;

    @Autowired
    private IdempotencyKeyStore store;

    @Test
    void store_then_lookup_returns_stored_claim_id() {
        UUID claimId = persistedClaim().id();

        store.store("ik_1", claimId);
        Optional<UUID> found = store.lookup("ik_1");

        assertThat(found).contains(claimId);
    }

    @Test
    void lookup_missing_key_returns_empty() {
        assertThat(store.lookup("ik_never")).isEmpty();
    }

    @Test
    void storing_same_key_twice_is_idempotent_and_keeps_first_mapping() {
        UUID first = persistedClaim().id();
        UUID second = persistedClaim().id();

        store.store("ik_dup", first);
        store.store("ik_dup", second);

        assertThat(store.lookup("ik_dup")).contains(first);
    }

    @Test
    void deleteOlderThan_removes_expired_keys_only() {
        UUID a = persistedClaim().id();
        UUID b = persistedClaim().id();
        store.store("ik_old", a);
        store.store("ik_recent", b);

        int removed = store.deleteOlderThan(NOW.plusSeconds(1));

        assertThat(removed).isEqualTo(2);
        assertThat(store.lookup("ik_old")).isEmpty();
        assertThat(store.lookup("ik_recent")).isEmpty();
    }

    private Claim persistedClaim() {
        Claim submitted = Claim.newSubmission("patient-42", CareType.DENTAL,
                new Money(new BigDecimal("50"), "EUR"), LocalDate.of(2026, 8, 15), CLOCK);
        return claims.save(submitted.decide(ClaimStatus.APPROVED, "auto", CLOCK));
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return CLOCK;
        }
    }
}
