package com.ecoapp.reward.dto;

import java.util.List;

/** Always contains 12 month entries (January to December), with zeros for months without returns. */
public record ReturnsSummaryResponse(
        int year,
        List<MonthlyReturnDto> months,
        int totalItems,
        double totalDepositEur,
        double totalPlasticKg
) {}
