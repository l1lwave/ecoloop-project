package com.ecoapp.scan.dto;

/** Answer of the internal deposit check: eligible means the stored deposit value is greater than zero. */
public record DepositCheckResponse(boolean eligible, double depositValue) {}
