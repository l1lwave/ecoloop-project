package com.ecoapp.location.dto;

import java.util.List;

public record LocationResponse(
        Long id,
        String name,
        String chain,
        String address,
        double lat,
        double lng,
        String openUntil,
        List<String> acceptedMaterials,
        int machineCount,
        double distanceMeters
) {}
