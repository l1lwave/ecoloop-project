package com.ecoapp.reward.exception;

/** Thrown for every claim rule violation. Deliberately carries no detail so callers cannot probe codes. */
public class InvalidReferralException extends RuntimeException {
    public InvalidReferralException() {
        super("Referral could not be claimed");
    }
}
