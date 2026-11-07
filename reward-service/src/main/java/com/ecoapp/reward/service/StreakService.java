package com.ecoapp.reward.service;

import com.ecoapp.reward.dto.CheckInResponse;
import com.ecoapp.reward.model.UserStats;
import com.ecoapp.reward.repository.DailyActivityRepository;
import com.ecoapp.reward.repository.UserStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Daily activity tracking and streak calculation (ADR 0001). The day is the Amsterdam local date. */
@Service
public class StreakService {

    private final DailyActivityRepository activityRepo;
    private final UserStatsRepository statsRepo;
    private final BadgeService badgeService;
    private final Clock clock;

    public StreakService(DailyActivityRepository activityRepo, UserStatsRepository statsRepo,
                         BadgeService badgeService, Clock clock) {
        this.activityRepo = activityRepo;
        this.statsRepo = statsRepo;
        this.badgeService = badgeService;
        this.clock = clock;
    }

    /**
     * Idempotent daily check-in. The stats row is locked first so concurrent check-ins and deposits of
     * the same user are serialized; a second call on the same day changes nothing.
     */
    @Transactional
    public CheckInResponse checkIn(String userId) {
        UserStats stats = statsRepo.findByIdForUpdate(userId)
                .orElseGet(() -> newStats(userId));

        boolean changed = recordActivity(userId, stats);
        List<String> newBadges = List.of();
        if (changed) {
            statsRepo.save(stats);
            newBadges = badgeService.checkAndUnlockBadges(userId, stats);
        }
        return new CheckInResponse(
                currentStreakFor(stats), stats.getLongestStreak(), true, newBadges);
    }

    /**
     * Records activity for today on the given (already loaded, ideally locked) stats entity and applies
     * the streak rule. Joins the caller's transaction and does NOT save the stats; the caller does.
     *
     * @return true if today was newly recorded, false if it was already recorded (no change made)
     */
    @Transactional
    public boolean recordActivity(String userId, UserStats stats) {
        LocalDate today = LocalDate.now(clock);
        // Native INSERT IGNORE: a duplicate means "already active today" without breaking the transaction.
        if (activityRepo.insertIfAbsent(userId, today) == 0) {
            return false;
        }
        LocalDate last = stats.getLastActiveDate();
        if (last != null && last.equals(today)) {
            return false; // defensive: activity row was missing but stats already counted today
        }
        int next = (last != null && last.equals(today.minusDays(1))) ? stats.getCurrentStreak() + 1 : 1;
        stats.setCurrentStreak(next);
        stats.setLongestStreak(Math.max(stats.getLongestStreak(), next));
        stats.setLastActiveDate(today);
        return true;
    }

    /**
     * Streak value to show: 0 if the last active day is older than yesterday. Read-only; the stored
     * value is corrected on the next activity.
     */
    public int currentStreakFor(UserStats stats) {
        LocalDate last = stats.getLastActiveDate();
        if (last == null) return 0;
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        return last.isBefore(yesterday) ? 0 : stats.getCurrentStreak();
    }

    private UserStats newStats(String userId) {
        UserStats s = new UserStats();
        s.setUserId(userId);
        return s;
    }
}
