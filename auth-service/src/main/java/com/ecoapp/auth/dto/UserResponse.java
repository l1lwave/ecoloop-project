package com.ecoapp.auth.dto;

import java.time.LocalDateTime;

public record UserResponse(
        String userId,
        String name,
        String email,
        LocalDateTime createdAt
) {}
