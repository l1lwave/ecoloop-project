package com.ecoapp.reward.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Format is validated before any lookup; must match the code alphabet used by ReferralService. */
public record ClaimReferralRequest(
        @NotNull
        @Pattern(regexp = "^[A-HJ-KM-NP-Z2-9]{8}$")
        String code
) {

    /**
     * Rejects any property other than "code". Spring Boot disables FAIL_ON_UNKNOWN_PROPERTIES globally,
     * so this DTO-scoped hook is the only way to reject unknown fields without changing shared Jackson config.
     * The thrown exception is mapped to the generic 400 INVALID_REQUEST body by GlobalExceptionHandler.
     */
    @JsonAnySetter
    public void rejectUnknownProperty(String name, Object value) throws JsonMappingException {
        throw new JsonMappingException(null, "Unknown property in claim request");
    }
}
