package com.elgourmat.careflow.domain;

import com.elgourmat.careflow.domain.exception.InvalidClaimAmountException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void constructs_valid_money() {
        Money money = new Money(new BigDecimal("99.99"), "EUR");

        assertThat(money.amount()).isEqualByComparingTo("99.99");
        assertThat(money.currency()).isEqualTo("EUR");
    }

    @Test
    void rejects_null_amount() {
        assertThatThrownBy(() -> new Money(null, "EUR"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void rejects_null_currency() {
        assertThatThrownBy(() -> new Money(new BigDecimal("10"), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void rejects_zero_amount() {
        assertThatThrownBy(() -> new Money(BigDecimal.ZERO, "EUR"))
                .isInstanceOf(InvalidClaimAmountException.class)
                .hasMessageContaining("strictly positive");
    }

    @Test
    void rejects_negative_amount() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1"), "EUR"))
                .isInstanceOf(InvalidClaimAmountException.class);
    }

    @Test
    void rejects_invalid_iso4217_currency() {
        assertThatThrownBy(() -> new Money(new BigDecimal("10"), "XYZ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 4217");
    }

    @Test
    void isLessThan_uses_compareTo_not_equals() {
        Money money = new Money(new BigDecimal("100.00"), "EUR");

        assertThat(money.isLessThan(new BigDecimal("100"))).isFalse();
        assertThat(money.isLessThan(new BigDecimal("100.01"))).isTrue();
    }

    @Test
    void isGreaterThan_uses_compareTo_not_equals() {
        Money money = new Money(new BigDecimal("500.00"), "EUR");

        assertThat(money.isGreaterThan(new BigDecimal("500"))).isFalse();
        assertThat(money.isGreaterThan(new BigDecimal("499.99"))).isTrue();
    }
}
