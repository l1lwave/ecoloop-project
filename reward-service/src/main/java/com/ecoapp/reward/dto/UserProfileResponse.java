package com.ecoapp.reward.dto;

import java.util.List;

public record UserProfileResponse(
        String userId,
        int totalPoints,
        int ecoSledLevel,
        double plasticRecycledKg,
        long waterSavedLiters,
        int locationsVisited,
        int ecoChecks,
        double weeklyPlasticKg,
        List<BadgeDto> badges,
        int currentStreak,
        int longestStreak
) {}
