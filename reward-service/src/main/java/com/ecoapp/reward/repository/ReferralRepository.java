package com.ecoapp.reward.repository;

import com.ecoapp.reward.model.Referral;
import com.ecoapp.reward.model.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    Optional<Referral> findByInvitedUserIdAndStatus(String invitedUserId, ReferralStatus status);

    long countByReferrerUserIdAndStatus(String referrerUserId, ReferralStatus status);

    /**
     * Inserts a pending referral unless the invited user already has one. Returns 1 if inserted, 0 if
     * the user was already invited (repeat claim). No exception, so the transaction is not poisoned.
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO referrals (invited_user_id, referrer_user_id, status, created_at) "
            + "VALUES (:invited, :referrer, 'PENDING', :now)", nativeQuery = true)
    int insertPendingIfAbsent(@Param("invited") String invitedUserId,
                              @Param("referrer") String referrerUserId,
                              @Param("now") LocalDateTime now);

    /**
     * Compare-and-set PENDING to CREDITED. Returns 1 only for the single caller that performs the
     * transition, which is what guarantees a referral cannot pay twice.
     */
    @Modifying
    @Query("update Referral r set r.status = com.ecoapp.reward.model.ReferralStatus.CREDITED, "
            + "r.creditedAt = :now where r.id = :id and r.status = com.ecoapp.reward.model.ReferralStatus.PENDING")
    int markCredited(@Param("id") Long id, @Param("now") LocalDateTime now);
}
