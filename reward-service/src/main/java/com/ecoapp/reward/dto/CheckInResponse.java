package com.ecoapp.reward.dto;

import java.util.List;

public record CheckInResponse(
        int currentStreak,
        int longestStreak,
        boolean checkedInToday,
        List<String> newBadges
) {}
