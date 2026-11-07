package com.ecoapp.reward.dto;

/** One calendar month of returns. Money is a deposit value in EUR, not a payout. */
public record MonthlyReturnDto(
        int month,
        int itemsReturned,
        double depositEur,
        double plasticKg
) {}
