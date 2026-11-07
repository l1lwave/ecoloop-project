package com.ecoapp.reward.exception;

import org.springframework.http.HttpStatus;

/** A deposit that must not be credited. Carries a fixed, generic client message. */
public class DepositRefusedException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String clientMessage;

    private DepositRefusedException(HttpStatus status, String code, String clientMessage) {
        super(code, null, false, false); // no stack trace needed, no sensitive data
        this.status = status;
        this.code = code;
        this.clientMessage = clientMessage;
    }

    public static DepositRefusedException unavailable() {
        return new DepositRefusedException(HttpStatus.SERVICE_UNAVAILABLE,
                "DEPOSIT_UNAVAILABLE", "Deposit could not be verified");
    }

    public static DepositRefusedException notReturnable() {
        return new DepositRefusedException(HttpStatus.BAD_REQUEST,
                "NOT_RETURNABLE", "This product cannot be returned for deposit");
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getClientMessage() { return clientMessage; }
}
