package com.ecoapp.reward.model;

import jakarta.persistence.*;

/** One referral code per user. The code is random and not derived from the user id (ADR 0002). */
@Entity
@Table(name = "referral_codes",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_referral_codes_user", columnNames = "user_id"),
           @UniqueConstraint(name = "uk_referral_codes_code", columnNames = "code")
       })
public class ReferralCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "code", nullable = false, length = 8)
    private String code;

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
