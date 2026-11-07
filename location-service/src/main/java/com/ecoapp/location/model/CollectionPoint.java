package com.ecoapp.location.model;

import jakarta.persistence.*;

@Entity
@Table(name = "collection_points")
public class CollectionPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 10)
    private String chain;

    @Column(nullable = false, length = 300, unique = true)
    private String address;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column(name = "open_until", length = 10)
    private String openUntil;

    // Comma-separated material codes e.g. "01 PET,Alu"
    @Column(name = "accepted_materials", length = 200)
    private String acceptedMaterials;

    @Column(name = "machine_count")
    private int machineCount = 1;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getChain() { return chain; }
    public void setChain(String chain) { this.chain = chain; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
    public String getOpenUntil() { return openUntil; }
    public void setOpenUntil(String openUntil) { this.openUntil = openUntil; }
    public String getAcceptedMaterials() { return acceptedMaterials; }
    public void setAcceptedMaterials(String acceptedMaterials) { this.acceptedMaterials = acceptedMaterials; }
    public int getMachineCount() { return machineCount; }
    public void setMachineCount(int machineCount) { this.machineCount = machineCount; }
}
