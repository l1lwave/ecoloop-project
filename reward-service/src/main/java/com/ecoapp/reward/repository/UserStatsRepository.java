package com.ecoapp.reward.repository;

import com.ecoapp.reward.model.UserStats;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserStatsRepository extends JpaRepository<UserStats, String> {

    /** Row-locking read so a deposit and a check-in of the same user cannot overwrite each other's changes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserStats s where s.userId = :userId")
    Optional<UserStats> findByIdForUpdate(@Param("userId") String userId);
}
