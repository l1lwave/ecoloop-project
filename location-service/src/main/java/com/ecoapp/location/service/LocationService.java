package com.ecoapp.location.service;

import com.ecoapp.location.dto.LocationResponse;
import com.ecoapp.location.model.CollectionPoint;
import com.ecoapp.location.repository.CollectionPointRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private final CollectionPointRepository repository;

    public LocationService(CollectionPointRepository repository) {
        this.repository = repository;
    }

    public List<LocationResponse> findNearby(double lat, double lng, double radiusMeters, String material) {
        List<CollectionPoint> all = material != null && !material.isBlank()
                ? repository.findByAcceptedMaterialsContaining(material)
                : repository.findAll();

        return all.stream()
                .map(p -> toResponse(p, lat, lng))
                .filter(r -> r.distanceMeters() <= radiusMeters)
                .sorted(Comparator.comparingDouble(LocationResponse::distanceMeters))
                .collect(Collectors.toList());
    }

    public LocationResponse findById(Long id, double lat, double lng) {
        CollectionPoint point = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        return toResponse(point, lat, lng);
    }

    private LocationResponse toResponse(CollectionPoint p, double userLat, double userLng) {
        double distance = haversine(userLat, userLng, p.getLat(), p.getLng());
        List<String> materials = p.getAcceptedMaterials() == null
                ? List.of()
                : Arrays.asList(p.getAcceptedMaterials().split(","));
        return new LocationResponse(
                p.getId(), p.getName(), p.getChain(), p.getAddress(),
                p.getLat(), p.getLng(), p.getOpenUntil(),
                materials, p.getMachineCount(), Math.round(distance)
        );
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
