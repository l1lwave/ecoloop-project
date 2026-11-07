package com.ecoapp.reward.repository;

import com.ecoapp.reward.model.ReferralCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, Long> {

    Optional<ReferralCode> findByUserId(String userId);

    Optional<ReferralCode> findByCode(String code);

    /**
     * Inserts the code unless the user already has one or the code is taken. Returns 1 if inserted, 0
     * otherwise. INSERT IGNORE reports a unique-key collision without raising an exception, so the
     * surrounding transaction stays usable and the caller can simply retry with a new code.
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO referral_codes (user_id, code) VALUES (:userId, :code)",
           nativeQuery = true)
    int insertIfAbsent(@Param("userId") String userId, @Param("code") String code);
}
