package com.ecoapp.scan.controller;

import com.ecoapp.scan.dto.DepositCheckResponse;
import com.ecoapp.scan.service.ScanService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service endpoint. It lives outside /api, which the gateway does not route, and is
 * protected by the shared token check in InternalTokenFilter.
 */
@RestController
@RequestMapping("/internal/scan")
public class InternalDepositController {

    private final ScanService scanService;

    public InternalDepositController(ScanService scanService) {
        this.scanService = scanService;
    }

    @GetMapping("/deposit/{barcode}")
    public ResponseEntity<DepositCheckResponse> deposit(
            @PathVariable @Pattern(regexp = "^[0-9]{8,14}$") String barcode) {
        return ResponseEntity.ok(scanService.checkDeposit(barcode));
    }
}
