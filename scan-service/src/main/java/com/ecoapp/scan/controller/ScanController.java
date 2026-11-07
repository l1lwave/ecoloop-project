package com.ecoapp.scan.controller;

import com.ecoapp.scan.dto.ScanHistoryResponse;
import com.ecoapp.scan.dto.ScanRequest;
import com.ecoapp.scan.dto.ScanResponse;
import com.ecoapp.scan.model.Material;
import com.ecoapp.scan.service.ScanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scan")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    /** Identity comes from the gateway-verified X-User-Id header only; never from the body. */
    @PostMapping("/identify")
    public ResponseEntity<ScanResponse> identify(@RequestHeader("X-User-Id") String userId,
                                                 @Valid @RequestBody ScanRequest request) {
        return ResponseEntity.ok(scanService.identify(request.barcode(), request.effectiveLang(), userId));
    }

    /** Size above the cap is reduced by the service; zero or negative values and negative pages are rejected. */
    @GetMapping("/history")
    public ResponseEntity<ScanHistoryResponse> history(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return ResponseEntity.ok(scanService.history(userId, page, size));
    }

    @GetMapping("/materials")
    public ResponseEntity<List<Material>> materials() {
        return ResponseEntity.ok(scanService.getAllMaterials());
    }
}
