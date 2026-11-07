package com.ecoapp.reward.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_stats")
public class UserStats {

    @Id
    @Column(length = 36)
    private String userId;

    @Column(name = "total_points", nullable = false)
    private int totalPoints = 0;

    @Column(name = "plastic_recycled_grams", nullable = false)
    private int plasticRecycledGrams = 0;

    @Column(name = "water_saved_ml", nullable = false)
    private long waterSavedMl = 0;

    @Column(name = "locations_visited", nullable = false)
    private int locationsVisited = 0;

    @Column(name = "eco_checks", nullable = false)
    private int ecoChecks = 0;

    @Column(name = "weekly_plastic_grams", nullable = false)
    private int weeklyPlasticGrams = 0;

    @Column(name = "current_streak", nullable = false, columnDefinition = "int default 0")
    private int currentStreak = 0;

    @Column(name = "longest_streak", nullable = false, columnDefinition = "int default 0")
    private int longestStreak = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int v) { this.currentStreak = v; }
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int v) { this.longestStreak = v; }
    public LocalDate getLastActiveDate() { return lastActiveDate; }
    public void setLastActiveDate(LocalDate v) { this.lastActiveDate = v; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    public int getPlasticRecycledGrams() { return plasticRecycledGrams; }
    public void setPlasticRecycledGrams(int v) { this.plasticRecycledGrams = v; }
    public long getWaterSavedMl() { return waterSavedMl; }
    public void setWaterSavedMl(long v) { this.waterSavedMl = v; }
    public int getLocationsVisited() { return locationsVisited; }
    public void setLocationsVisited(int v) { this.locationsVisited = v; }
    public int getEcoChecks() { return ecoChecks; }
    public void setEcoChecks(int v) { this.ecoChecks = v; }
    public int getWeeklyPlasticGrams() { return weeklyPlasticGrams; }
    public void setWeeklyPlasticGrams(int v) { this.weeklyPlasticGrams = v; }

    public int getEcoSledLevel() {
        if (totalPoints >= 5000) return 10;
        if (totalPoints >= 3000) return 8;
        if (totalPoints >= 2000) return 7;
        if (totalPoints >= 1500) return 6;
        if (totalPoints >= 1000) return 5;
        if (totalPoints >= 700)  return 4;
        if (totalPoints >= 400)  return 3;
        if (totalPoints >= 200)  return 2;
        if (totalPoints >= 50)   return 1;
        return 0;
    }
}
