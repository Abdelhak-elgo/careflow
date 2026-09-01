package com.elgourmat.careflow.domain.exception;

public class InvalidClaimAmountException extends RuntimeException {

    public InvalidClaimAmountException(String message) {
        super(message);
    }
}
