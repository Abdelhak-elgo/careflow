package com.elgourmat.careflow.domain;

import com.elgourmat.careflow.domain.exception.InvalidClaimAmountException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");
        if (amount.signum() <= 0) {
            throw new InvalidClaimAmountException("amount must be strictly positive, was: " + amount);
        }
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("currency must be a valid ISO 4217 code, was: " + currency, e);
        }
    }

    public boolean isLessThan(BigDecimal threshold) {
        return amount.compareTo(threshold) < 0;
    }

    public boolean isGreaterThan(BigDecimal threshold) {
        return amount.compareTo(threshold) > 0;
    }

    public boolean isCurrency(String isoCode) {
        return currency.equals(isoCode);
    }
}
