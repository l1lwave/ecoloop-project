package com.ecoapp.reward.controller;

import com.ecoapp.reward.dto.CheckInResponse;
import com.ecoapp.reward.service.RewardService;
import com.ecoapp.reward.service.StreakService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RewardControllerTest {

    private final RewardService rewardService = mock(RewardService.class);
    private final StreakService streakService = mock(StreakService.class);
    private final RewardController controller = new RewardController(rewardService, streakService);

    @Test
    void checkIn_usesHeaderIdentityAndReturnsContract() {
        CheckInResponse body = new CheckInResponse(5, 12, true, List.of("eco-family"));
        when(streakService.checkIn("u1")).thenReturn(body);

        ResponseEntity<CheckInResponse> r = controller.checkIn("u1");

        assertEquals(200, r.getStatusCode().value());
        assertEquals(body, r.getBody());
    }
}
