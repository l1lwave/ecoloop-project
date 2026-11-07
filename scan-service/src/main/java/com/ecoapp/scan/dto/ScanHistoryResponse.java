package com.ecoapp.scan.dto;

import java.util.List;

public record ScanHistoryResponse(
        List<ScanHistoryItem> items,
        int page,
        int size,
        boolean hasMore
) {}
