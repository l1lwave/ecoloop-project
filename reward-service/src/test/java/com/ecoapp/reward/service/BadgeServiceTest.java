package com.ecoapp.reward.service;

import com.ecoapp.reward.model.Badge;
import com.ecoapp.reward.model.UserBadge;
import com.ecoapp.reward.model.UserStats;
import com.ecoapp.reward.repository.BadgeRepository;
import com.ecoapp.reward.repository.DepositRecordRepository;
import com.ecoapp.reward.repository.UserBadgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock BadgeRepository badgeRepo;
    @Mock UserBadgeRepository userBadgeRepo;
    @Mock DepositRecordRepository depositRepo;
    @Mock com.ecoapp.reward.repository.ReferralRepository referralRepo;
    @InjectMocks BadgeService service;

    private Badge badge(String id, int points, int deposits, int locations, int streak) {
        Badge b = new Badge();
        b.setId(id);
        b.setRequiredPoints(points);
        b.setRequiredDeposits(deposits);
        b.setRequiredLocations(locations);
        b.setRequiredStreakDays(streak);
        return b;
    }

    @Test
    void referralBadge_unlocksAtThreeCreditedReferrals() {
        Badge b = badge("eco-ambassador", 0, 0, 0, 0);
        b.setRequiredReferrals(3);
        when(userBadgeRepo.findBadgeIdsByUserId("u1")).thenReturn(Set.of());
        when(depositRepo.countByUserId("u1")).thenReturn(0L);
        when(badgeRepo.findAll()).thenReturn(List.of(b));
        when(referralRepo.countByReferrerUserIdAndStatus("u1", com.ecoapp.reward.model.ReferralStatus.CREDITED))
                .thenReturn(3L);

        assertEquals(List.of("eco-ambassador"), service.checkAndUnlockBadges("u1", new UserStats()));
    }

    @Test
    void referralBadge_notUnlockedBelowThree() {
        Badge b = badge("eco-ambassador", 0, 0, 0, 0);
        b.setRequiredReferrals(3);
        when(userBadgeRepo.findBadgeIdsByUserId("u1")).thenReturn(Set.of());
        when(depositRepo.countByUserId("u1")).thenReturn(0L);
        when(badgeRepo.findAll()).thenReturn(List.of(b));
        when(referralRepo.countByReferrerUserIdAndStatus("u1", com.ecoapp.reward.model.ReferralStatus.CREDITED))
                .thenReturn(2L);

        assertTrue(service.checkAndUnlockBadges("u1", new UserStats()).isEmpty());
    }

    @Test
    void streakBadge_unlocksAtThirtyDays() {
        UserStats s = new UserStats();
        s.setCurrentStreak(30);
        when(userBadgeRepo.findBadgeIdsByUserId("u1")).thenReturn(Set.of());
        when(depositRepo.countByUserId("u1")).thenReturn(0L);
        when(badgeRepo.findAll()).thenReturn(List.of(badge("eco-family", 0, 0, 0, 30)));

        assertEquals(List.of("eco-family"), service.checkAndUnlockBadges("u1", s));
        verify(userBadgeRepo).save(any(UserBadge.class));
    }

    @Test
    void streakBadge_notUnlockedBelowThirtyOrByDeposits() {
        UserStats s = new UserStats();
        s.setCurrentStreak(29);
        when(userBadgeRepo.findBadgeIdsByUserId("u1")).thenReturn(Set.of());
        when(depositRepo.countByUserId("u1")).thenReturn(50L);
        when(badgeRepo.findAll()).thenReturn(List.of(badge("eco-family", 0, 0, 0, 30)));

        assertTrue(service.checkAndUnlockBadges("u1", s).isEmpty());
        verify(userBadgeRepo, never()).save(any());
    }

    @Test
    void alreadyHeldBadge_isNotUnlockedAgain() {
        UserStats s = new UserStats();
        s.setCurrentStreak(40);
        when(userBadgeRepo.findBadgeIdsByUserId("u1")).thenReturn(Set.of("eco-family"));
        when(depositRepo.countByUserId("u1")).thenReturn(0L);
        when(badgeRepo.findAll()).thenReturn(List.of(badge("eco-family", 0, 0, 0, 30)));

        assertTrue(service.checkAndUnlockBadges("u1", s).isEmpty());
    }

    @Test
    void existingCriteria_stillWork() {
        UserStats s = new UserStats();
        s.setTotalPoints(250);
        s.setLocationsVisited(3);
        when(userBadgeRepo.findBadgeIdsByUserId("u1")).thenReturn(Set.of());
        when(depositRepo.countByUserId("u1")).thenReturn(10L);
        when(badgeRepo.findAll()).thenReturn(List.of(
                badge("p", 200, 0, 0, 0), badge("d", 0, 10, 0, 0), badge("l", 0, 0, 3, 0)));

        assertEquals(List.of("p", "d", "l"), service.checkAndUnlockBadges("u1", s));
    }
}
