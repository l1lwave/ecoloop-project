package com.ecoapp.reward.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_records")
public class DepositRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "location_id", length = 50)
    private String locationId;

    @Column(length = 50)
    private String barcode;

    @Column(name = "material_type", length = 20)
    private String materialType;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    /** Deposit value in EUR as confirmed by scan-service; null for rows from before verification. */
    @Column(name = "deposit_value")
    private Double depositValue;

    @Column(name = "deposited_at", nullable = false)
    private LocalDateTime depositedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }
    public Double getDepositValue() { return depositValue; }
    public void setDepositValue(Double depositValue) { this.depositValue = depositValue; }
    public LocalDateTime getDepositedAt() { return depositedAt; }
}
