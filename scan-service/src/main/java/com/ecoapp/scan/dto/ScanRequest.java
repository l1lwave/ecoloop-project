package com.ecoapp.scan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ScanRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9]{8,14}$", message = "Barcode must be 8–14 digits (EAN-8, EAN-13, or GTIN-14)")
        String barcode,
        /** UI locale — nl | en | ua  (maps to OFF fields: nl, en, uk) */
        String lang
) {
    public String effectiveLang() {
        if (lang == null || lang.isBlank()) return "nl";
        return lang.trim().toLowerCase();
    }
}
