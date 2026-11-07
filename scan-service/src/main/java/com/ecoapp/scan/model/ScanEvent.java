package com.ecoapp.scan.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** One row per successful identify for an authenticated user. */
@Entity
@Table(name = "scan_events",
        indexes = @Index(name = "idx_scan_events_user_scanned", columnList = "user_id, scanned_at"))
public class ScanEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Taken from the gateway X-User-Id header only. */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 50)
    private String barcode;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "material_code", length = 20)
    private String materialCode;

    @Column(name = "deposit_value")
    private double depositValue;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public double getDepositValue() { return depositValue; }
    public void setDepositValue(double depositValue) { this.depositValue = depositValue; }
    public LocalDateTime getScannedAt() { return scannedAt; }
    public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }
}
