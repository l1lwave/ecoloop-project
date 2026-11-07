package com.ecoapp.reward.controller;

import com.ecoapp.reward.dto.ClaimReferralRequest;
import com.ecoapp.reward.dto.ClaimReferralResponse;
import com.ecoapp.reward.dto.ReferralMeResponse;
import com.ecoapp.reward.exception.InvalidReferralException;
import com.ecoapp.reward.exception.GlobalExceptionHandler;
import com.ecoapp.reward.service.ReferralService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReferralControllerTest {

    private final ReferralService service = mock(ReferralService.class);
    private final ReferralController controller = new ReferralController(service);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void me_usesHeaderIdentity() {
        ReferralMeResponse body = new ReferralMeResponse("K7M2QX9P", 2, 1, false);
        when(service.getSummary("u1")).thenReturn(body);

        ResponseEntity<ReferralMeResponse> r = controller.me("u1");

        assertEquals(200, r.getStatusCode().value());
        assertEquals(body, r.getBody());
    }

    @Test
    void claim_returnsClaimedTrue() {
        ResponseEntity<ClaimReferralResponse> r = controller.claim("u1", new ClaimReferralRequest("K7M2QX9P"));

        assertEquals(200, r.getStatusCode().value());
        assertTrue(r.getBody().claimed());
        verify(service).claim("u1", "K7M2QX9P");
    }

    @Test
    void claim_ruleViolationPropagates() {
        doThrow(new InvalidReferralException()).when(service).claim("u1", "K7M2QX9P");

        assertThrows(InvalidReferralException.class,
                () -> controller.claim("u1", new ClaimReferralRequest("K7M2QX9P")));
    }

    @Test
    void handler_mapsInvalidReferralToGenericBadRequest() {
        ResponseEntity<Map<String, String>> r =
                new GlobalExceptionHandler().handleInvalidReferral(new InvalidReferralException());

        assertEquals(400, r.getStatusCode().value());
        assertEquals(Map.of("error", "INVALID_REFERRAL", "message", "Referral could not be claimed"), r.getBody());
    }

    @Test
    void request_validatesFormatAndLength() {
        assertTrue(validator.validate(new ClaimReferralRequest("K7M2QX9P")).isEmpty());
        assertFalse(validator.validate(new ClaimReferralRequest(null)).isEmpty());
        assertFalse(validator.validate(new ClaimReferralRequest("")).isEmpty());
        assertFalse(validator.validate(new ClaimReferralRequest("K7M2QX9")).isEmpty());
        assertFalse(validator.validate(new ClaimReferralRequest("K7M2QX9PP")).isEmpty());
        assertFalse(validator.validate(new ClaimReferralRequest("K7M2QX90")).isEmpty()); // ambiguous 0
        assertFalse(validator.validate(new ClaimReferralRequest("k7m2qx9p")).isEmpty());
        assertFalse(validator.validate(new ClaimReferralRequest("K7M2QX9'")).isEmpty());
    }
}
