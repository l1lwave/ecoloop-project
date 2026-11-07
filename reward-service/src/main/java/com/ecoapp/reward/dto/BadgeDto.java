package com.ecoapp.reward.dto;

public record BadgeDto(
        String id,
        String name,
        String description,
        boolean unlocked
) {}
