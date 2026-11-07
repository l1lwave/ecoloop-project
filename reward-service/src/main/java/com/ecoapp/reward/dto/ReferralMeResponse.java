package com.ecoapp.reward.dto;

/** The caller's own referral summary. Counts only; invited users are never identified. */
public record ReferralMeResponse(String code, long creditedCount, long pendingCount, boolean ambassadorUnlocked) {}
