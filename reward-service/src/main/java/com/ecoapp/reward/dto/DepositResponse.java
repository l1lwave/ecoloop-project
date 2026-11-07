package com.ecoapp.reward.dto;

import java.util.List;

public record DepositResponse(
        int pointsEarned,
        int totalPoints,
        int ecoSledLevel,
        List<String> newBadges,
        double plasticRecycledKg,
        long waterSavedLiters
) {}
