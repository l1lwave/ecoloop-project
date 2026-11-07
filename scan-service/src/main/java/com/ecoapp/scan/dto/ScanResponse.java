package com.ecoapp.scan.dto;

public record ScanResponse(
        String barcode,
        String productName,
        String brand,
        String materialType,
        String materialLabel,
        boolean recyclable,
        double depositValue,
        String instructions
) {}
