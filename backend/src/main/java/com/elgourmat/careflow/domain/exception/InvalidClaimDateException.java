package com.elgourmat.careflow.domain.exception;

public class InvalidClaimDateException extends RuntimeException {

    public InvalidClaimDateException(String message) {
        super(message);
    }
}
