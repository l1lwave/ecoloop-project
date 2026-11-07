package com.ecoapp.reward.controller;

import com.ecoapp.reward.dto.ReturnsSummaryQuery;
import com.ecoapp.reward.dto.ReturnsSummaryResponse;
import com.ecoapp.reward.service.ReturnsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rewards")
public class ReturnsController {

    private final ReturnsService returnsService;

    public ReturnsController(ReturnsService returnsService) {
        this.returnsService = returnsService;
    }

    // Identity from the gateway-verified header only; the year is the only query parameter.
    @GetMapping("/returns/summary")
    public ResponseEntity<ReturnsSummaryResponse> summary(
            @RequestHeader("X-User-Id") String userId,
            @Valid @ModelAttribute ReturnsSummaryQuery query) {
        // The pattern on the query guarantees a 4-digit value, so parsing cannot fail here.
        return ResponseEntity.ok(returnsService.getSummary(userId, Integer.parseInt(query.year())));
    }
}
