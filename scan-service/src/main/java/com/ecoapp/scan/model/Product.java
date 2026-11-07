package com.ecoapp.scan.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(length = 50)
    private String barcode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "material_code", nullable = false, length = 20)
    private String materialCode;

    @Column(name = "deposit_value", nullable = false)
    private double depositValue;

    @Column(name = "volume_ml")
    private Integer volumeMl;

    @Column(length = 100)
    private String brand;

    /**
     * Whether Open Food Facts classified the product as a beverage.
     * Null means the row was cached before this flag existed and must be re-classified.
     */
    @Column(name = "beverage")
    private Boolean beverage;

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public double getDepositValue() { return depositValue; }
    public void setDepositValue(double depositValue) { this.depositValue = depositValue; }
    public Integer getVolumeMl() { return volumeMl; }
    public void setVolumeMl(Integer volumeMl) { this.volumeMl = volumeMl; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Boolean getBeverage() { return beverage; }
    public void setBeverage(Boolean beverage) { this.beverage = beverage; }
}
