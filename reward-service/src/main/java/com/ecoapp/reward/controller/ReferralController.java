package com.ecoapp.reward.controller;

import com.ecoapp.reward.dto.ClaimReferralRequest;
import com.ecoapp.reward.dto.ClaimReferralResponse;
import com.ecoapp.reward.dto.ReferralMeResponse;
import com.ecoapp.reward.service.ReferralService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Identity comes from the gateway-verified X-User-Id header only; never from the path or body. */
@RestController
@RequestMapping("/api/rewards/referrals")
public class ReferralController {

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @GetMapping("/me")
    public ResponseEntity<ReferralMeResponse> me(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(referralService.getSummary(userId));
    }

    @PostMapping("/claim")
    public ResponseEntity<ClaimReferralResponse> claim(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ClaimReferralRequest request) {
        referralService.claim(userId, request.code());
        return ResponseEntity.ok(new ClaimReferralResponse(true));
    }
}
