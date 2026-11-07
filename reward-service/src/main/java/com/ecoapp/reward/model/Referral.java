package com.ecoapp.reward.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A claimed referral. The unique key on the invited user allows each user to be invited only once. */
@Entity
@Table(name = "referrals",
       uniqueConstraints = @UniqueConstraint(name = "uk_referrals_invited_user", columnNames = "invited_user_id"),
       indexes = @Index(name = "idx_referrals_referrer_status", columnList = "referrer_user_id, status"))
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invited_user_id", nullable = false, length = 36)
    private String invitedUserId;

    @Column(name = "referrer_user_id", nullable = false, length = 36)
    private String referrerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "credited_at")
    private LocalDateTime creditedAt;

    public Long getId() { return id; }
    public String getInvitedUserId() { return invitedUserId; }
    public void setInvitedUserId(String v) { this.invitedUserId = v; }
    public String getReferrerUserId() { return referrerUserId; }
    public void setReferrerUserId(String v) { this.referrerUserId = v; }
    public ReferralStatus getStatus() { return status; }
    public void setStatus(ReferralStatus v) { this.status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getCreditedAt() { return creditedAt; }
    public void setCreditedAt(LocalDateTime v) { this.creditedAt = v; }
}
