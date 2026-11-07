package com.ecoapp.reward.repository;

import com.ecoapp.reward.model.DailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface DailyActivityRepository extends JpaRepository<DailyActivity, Long> {

    /**
     * Inserts the activity row unless one already exists for (user, date).
     * Returns 1 if inserted, 0 if the day was already recorded. A plain save() would raise a
     * constraint violation that marks the surrounding transaction rollback-only and poisons the
     * Hibernate session; INSERT IGNORE on the unique key reports the duplicate without an exception.
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO daily_activity (user_id, activity_date) VALUES (:userId, :date)",
           nativeQuery = true)
    int insertIfAbsent(@Param("userId") String userId, @Param("date") LocalDate date);

    boolean existsByUserIdAndActivityDate(String userId, LocalDate activityDate);
}
