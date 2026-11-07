package com.ecoapp.reward.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Query parameters for the monthly return summary. The year is a 4-digit value from 2000 to 2100. */
public record ReturnsSummaryQuery(
        @NotBlank
        @Pattern(regexp = "^(20[0-9]{2}|2100)$")
        String year
) {}
