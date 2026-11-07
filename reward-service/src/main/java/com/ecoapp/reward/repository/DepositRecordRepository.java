package com.ecoapp.reward.repository;

import com.ecoapp.reward.model.DepositRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DepositRecordRepository extends JpaRepository<DepositRecord, Long> {
    List<DepositRecord> findByUserId(String userId);
    long countByUserId(String userId);

    /** Deposit records of one user with deposited_at in [start, end). Parameterized; used by the monthly return summary. */
    @Query("select r from DepositRecord r where r.userId = :userId and r.depositedAt >= :start and r.depositedAt < :end")
    List<DepositRecord> findReturnsBetween(@Param("userId") String userId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);
}
