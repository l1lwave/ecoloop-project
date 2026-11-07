package com.ecoapp.reward.service;

import com.ecoapp.reward.dto.CheckInResponse;
import com.ecoapp.reward.model.UserStats;
import com.ecoapp.reward.repository.DailyActivityRepository;
import com.ecoapp.reward.repository.UserStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    private static final ZoneId AMS = ZoneId.of("Europe/Amsterdam");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);

    @Mock DailyActivityRepository activityRepo;
    @Mock UserStatsRepository statsRepo;
    @Mock BadgeService badgeService;

    private StreakService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(TODAY.atTime(12, 0).atZone(AMS).toInstant());
    }

    private StreakService serviceAt(Instant instant) {
        return new StreakService(activityRepo, statsRepo, badgeService, Clock.fixed(instant, AMS));
    }

    private UserStats stats(int current, int longest, LocalDate last) {
        UserStats s = new UserStats();
        s.setUserId("u1");
        s.setCurrentStreak(current);
        s.setLongestStreak(longest);
        s.setLastActiveDate(last);
        return s;
    }

    @Test
    void checkIn_firstEverActivity_startsStreakAtOne() {
        when(statsRepo.findByIdForUpdate("u1")).thenReturn(Optional.empty());
        when(activityRepo.insertIfAbsent("u1", TODAY)).thenReturn(1);
        when(badgeService.checkAndUnlockBadges(eq("u1"), any())).thenReturn(List.of());

        CheckInResponse r = service.checkIn("u1");

        assertEquals(1, r.currentStreak());
        assertEquals(1, r.longestStreak());
        assertTrue(r.checkedInToday());
        verify(statsRepo).save(any(UserStats.class));
    }

    @Test
    void checkIn_yesterdayActive_incrementsStreakAndUpdatesLongest() {
        UserStats s = stats(4, 4, TODAY.minusDays(1));
        when(statsRepo.findByIdForUpdate("u1")).thenReturn(Optional.of(s));
        when(activityRepo.insertIfAbsent("u1", TODAY)).thenReturn(1);
        when(badgeService.checkAndUnlockBadges("u1", s)).thenReturn(List.of("eco-family"));

        CheckInResponse r = service.checkIn("u1");

        assertEquals(5, r.currentStreak());
        assertEquals(5, r.longestStreak());
        assertEquals(List.of("eco-family"), r.newBadges());
        assertEquals(TODAY, s.getLastActiveDate());
    }

    @Test
    void checkIn_gap_restartsAtOneAndKeepsLongest() {
        UserStats s = stats(9, 12, TODAY.minusDays(3));
        when(statsRepo.findByIdForUpdate("u1")).thenReturn(Optional.of(s));
        when(activityRepo.insertIfAbsent("u1", TODAY)).thenReturn(1);
        when(badgeService.checkAndUnlockBadges("u1", s)).thenReturn(List.of());

        CheckInResponse r = service.checkIn("u1");

        assertEquals(1, r.currentStreak());
        assertEquals(12, r.longestStreak());
    }

    @Test
    void checkIn_alreadyCheckedInToday_changesNothing() {
        UserStats s = stats(5, 12, TODAY);
        when(statsRepo.findByIdForUpdate("u1")).thenReturn(Optional.of(s));
        when(activityRepo.insertIfAbsent("u1", TODAY)).thenReturn(0);

        CheckInResponse r = service.checkIn("u1");

        assertEquals(5, r.currentStreak());
        assertEquals(12, r.longestStreak());
        assertTrue(r.checkedInToday());
        assertTrue(r.newBadges().isEmpty());
        verify(statsRepo, never()).save(any());
        verifyNoInteractions(badgeService);
    }

    @Test
    void checkIn_usesAmsterdamDateNotUtc() {
        // 23:30 UTC on 25 Oct is already 26 Oct 00:30 in Amsterdam (CET after DST end: UTC+1).
        service = serviceAt(Instant.parse("2026-10-25T23:30:00Z"));
        when(statsRepo.findByIdForUpdate("u1")).thenReturn(Optional.empty());
        when(activityRepo.insertIfAbsent("u1", TODAY)).thenReturn(1);

        service.checkIn("u1");

        verify(activityRepo).insertIfAbsent("u1", TODAY);
    }

    @Test
    void recordActivity_newDay_appliesRuleWithoutSaving() {
        UserStats s = stats(2, 2, TODAY.minusDays(1));
        when(activityRepo.insertIfAbsent("u1", TODAY)).thenReturn(1);

        assertTrue(service.recordActivity("u1", s));

        assertEquals(3, s.getCurrentStreak());
        verify(statsRepo, never()).save(any());
    }

    @Test
    void recordActivity_duplicate_returnsFalseAndLeavesStats() {
        UserStats s = stats(2, 2, TODAY);
        when(activityRepo.insertIfAbsent("u1", TODAY)).thenReturn(0);

        assertFalse(service.recordActivity("u1", s));
        assertEquals(2, s.getCurrentStreak());
    }

    @Test
    void recordActivity_statsAlreadyCountToday_doesNotDoubleCount() {
        UserStats s = stats(2, 2, TODAY);
        when(activityRepo.insertIfAbsent("u1", TODAY)).thenReturn(1);

        assertFalse(service.recordActivity("u1", s));
        assertEquals(2, s.getCurrentStreak());
    }

    @Test
    void currentStreakFor_neverActive_isZero() {
        assertEquals(0, service.currentStreakFor(stats(0, 0, null)));
    }

    @Test
    void currentStreakFor_todayOrYesterday_returnsStored() {
        assertEquals(7, service.currentStreakFor(stats(7, 9, TODAY)));
        assertEquals(7, service.currentStreakFor(stats(7, 9, TODAY.minusDays(1))));
    }

    @Test
    void currentStreakFor_olderThanYesterday_isZeroWithoutMutation() {
        UserStats s = stats(7, 9, TODAY.minusDays(2));

        assertEquals(0, service.currentStreakFor(s));
        assertEquals(7, s.getCurrentStreak());
    }
}
