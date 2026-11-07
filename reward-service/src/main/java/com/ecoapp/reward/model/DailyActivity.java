package com.ecoapp.reward.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_activity",
       uniqueConstraints = @UniqueConstraint(name = "uk_daily_activity_user_date",
                                             columnNames = {"user_id", "activity_date"}))
public class DailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate activityDate) { this.activityDate = activityDate; }
}
