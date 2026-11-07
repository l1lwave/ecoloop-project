package com.ecoapp.reward.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** userId is intentionally NOT a field here — it must come from the gateway-verified
 *  X-User-Id header (see RewardController), never from client-supplied request data,
 *  or any authenticated user could credit points/deposits to an arbitrary account. */
public record DepositRequest(
        String locationId,
        @NotBlank
        @Pattern(regexp = "^[0-9]{8,14}$")
        String barcode,
        @NotBlank String materialType
) {}
