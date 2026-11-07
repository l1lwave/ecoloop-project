package com.ecoapp.reward.controller;

import com.ecoapp.reward.dto.CheckInResponse;
import com.ecoapp.reward.dto.DepositRequest;
import com.ecoapp.reward.dto.DepositResponse;
import com.ecoapp.reward.dto.UserProfileResponse;
import com.ecoapp.reward.service.RewardService;
import com.ecoapp.reward.service.StreakService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    private final RewardService rewardService;

    private final StreakService streakService;

    public RewardController(RewardService rewardService, StreakService streakService) {
        this.rewardService = rewardService;
        this.streakService = streakService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(rewardService.processDeposit(userId, request));
    }

    // Identity from the gateway-verified header only; no path variable and no request body.
    @PostMapping("/checkin")
    public ResponseEntity<CheckInResponse> checkIn(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(streakService.checkIn(userId));
    }

    // No {userId} path variable on purpose — this must only ever return the
    // caller's OWN profile (from the gateway-verified header), never anyone else's.
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> profile(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(rewardService.getProfile(userId));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserProfileResponse>> leaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(rewardService.getLeaderboard(limit));
    }
}
