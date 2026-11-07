package com.ecoapp.reward.model;

import jakarta.persistence.*;

@Entity
@Table(name = "badges")
public class Badge {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(name = "required_points")
    private int requiredPoints;

    @Column(name = "required_deposits")
    private int requiredDeposits;

    @Column(name = "required_locations")
    private int requiredLocations;

    @Column(name = "required_streak_days", nullable = false, columnDefinition = "int default 0")
    private int requiredStreakDays = 0;

    @Column(name = "required_referrals", nullable = false, columnDefinition = "int default 0")
    private int requiredReferrals = 0;

    public int getRequiredReferrals() { return requiredReferrals; }
    public void setRequiredReferrals(int v) { this.requiredReferrals = v; }

    public int getRequiredStreakDays() { return requiredStreakDays; }
    public void setRequiredStreakDays(int v) { this.requiredStreakDays = v; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getRequiredPoints() { return requiredPoints; }
    public void setRequiredPoints(int v) { this.requiredPoints = v; }
    public int getRequiredDeposits() { return requiredDeposits; }
    public void setRequiredDeposits(int v) { this.requiredDeposits = v; }
    public int getRequiredLocations() { return requiredLocations; }
    public void setRequiredLocations(int v) { this.requiredLocations = v; }
}
