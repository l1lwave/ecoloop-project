package com.ecoapp.scan.dto;

import java.time.LocalDateTime;

public record ScanHistoryItem(
        String barcode,
        String productName,
        String materialLabel,
        boolean recyclable,
        double depositValue,
        LocalDateTime scannedAt
) {}
