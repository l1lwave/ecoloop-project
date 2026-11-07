package com.ecoapp.location.controller;

import com.ecoapp.location.dto.LocationResponse;
import com.ecoapp.location.service.LocationService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Validated
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public ResponseEntity<List<LocationResponse>> nearby(
            @RequestParam(defaultValue = "52.3676") @DecimalMin("-90")  @DecimalMax("90")  double lat,
            @RequestParam(defaultValue = "4.9041")  @DecimalMin("-180") @DecimalMax("180") double lng,
            @RequestParam(defaultValue = "5000")    @Positive @Max(50_000) double radius,
            @RequestParam(required = false)         String material) {
        return ResponseEntity.ok(locationService.findNearby(lat, lng, radius, material));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> byId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "52.3676") @DecimalMin("-90")  @DecimalMax("90")  double lat,
            @RequestParam(defaultValue = "4.9041")  @DecimalMin("-180") @DecimalMax("180") double lng) {
        return ResponseEntity.ok(locationService.findById(id, lat, lng));
    }
}
