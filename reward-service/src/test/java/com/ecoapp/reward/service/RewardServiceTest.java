package com.ecoapp.reward.service;

import com.ecoapp.reward.dto.DepositRequest;
import com.ecoapp.reward.dto.DepositResponse;
import com.ecoapp.reward.dto.UserProfileResponse;
import com.ecoapp.reward.model.Badge;
import com.ecoapp.reward.model.DepositRecord;
import com.ecoapp.reward.model.UserStats;
import com.ecoapp.reward.repository.*;
import com.ecoapp.reward.client.ScanDepositClient;
import com.ecoapp.reward.exception.DepositRefusedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock UserStatsRepository statsRepo;
    @Mock BadgeRepository badgeRepo;
    @Mock UserBadgeRepository userBadgeRepo;
    @Mock DepositRecordRepository depositRepo;
    @Mock BadgeService badgeService;
    @Mock StreakService streakService;
    @Mock ReferralService referralService;
    @Mock ScanDepositClient scanClient;
    @Mock PlatformTransactionManager txManager;

    private RewardService service;

    @BeforeEach
    void setUp() {
        service = new RewardService(statsRepo, badgeRepo, userBadgeRepo, depositRepo, badgeService,
                streakService, referralService, scanClient, new TransactionTemplate(txManager));
    }

    private static final String BARCODE = "8718951234567";

    private void eligible() {
        when(scanClient.check(BARCODE)).thenReturn(new ScanDepositClient.DepositCheck(true, 0.15));
    }

    private void verifyNoWrites() {
        verifyNoInteractions(statsRepo, depositRepo, streakService, badgeService, referralService, txManager);
    }

    @Test
    void processDeposit_eligibleStoresScanServiceValue() {
        eligible();
        when(statsRepo.findByIdForUpdate("u3")).thenReturn(Optional.empty());
        when(badgeService.checkAndUnlockBadges(eq("u3"), any())).thenReturn(List.of());

        DepositResponse r = service.processDeposit("u3", new DepositRequest("loc", BARCODE, "PET"));

        assertEquals(50, r.pointsEarned());
        ArgumentCaptor<DepositRecord> cap = ArgumentCaptor.forClass(DepositRecord.class);
        verify(depositRepo).save(cap.capture());
        assertEquals(0.15, cap.getValue().getDepositValue(), 0.0001);
        assertEquals(BARCODE, cap.getValue().getBarcode());
    }

    @Test
    void processDeposit_notReturnableRefusesWithNoWrites() {
        when(scanClient.check(BARCODE)).thenReturn(new ScanDepositClient.DepositCheck(false, 0.0));

        DepositRefusedException e = assertThrows(DepositRefusedException.class,
                () -> service.processDeposit("u1", new DepositRequest("loc", BARCODE, "PET")));

        assertEquals(400, e.getStatus().value());
        assertEquals("NOT_RETURNABLE", e.getCode());
        verifyNoWrites();
    }

    @Test
    void processDeposit_scanUnavailableRefusesWith503AndNoWrites() {
        when(scanClient.check(BARCODE)).thenThrow(DepositRefusedException.unavailable());

        DepositRefusedException e = assertThrows(DepositRefusedException.class,
                () -> service.processDeposit("u1", new DepositRequest("loc", BARCODE, "PET")));

        assertEquals(503, e.getStatus().value());
        assertEquals("DEPOSIT_UNAVAILABLE", e.getCode());
        verifyNoWrites();
    }

    @Test
    void processDeposit_recordsActivityAndChecksBadges() {
        eligible();
        UserStats s = new UserStats();
        s.setUserId("u1");
        when(statsRepo.findByIdForUpdate("u1")).thenReturn(Optional.of(s));
        when(badgeService.checkAndUnlockBadges("u1", s)).thenReturn(List.of("first-checkin"));

        DepositResponse r = service.processDeposit("u1", new DepositRequest("loc", BARCODE, "PET"));

        assertEquals(50, r.pointsEarned());
        assertEquals(List.of("first-checkin"), r.newBadges());
        InOrder order = inOrder(streakService, statsRepo, badgeService);
        order.verify(streakService).recordActivity("u1", s);
        verify(referralService).lockParticipants("u1");
        verify(referralService).creditOnFirstDeposit("u1", s);
        order.verify(statsRepo).save(s);
        order.verify(badgeService).checkAndUnlockBadges("u1", s);
        verify(depositRepo).save(any(DepositRecord.class));
    }

    @Test
    void processDeposit_aluminiumGivesPointsForNewUser() {
        eligible();
        when(statsRepo.findByIdForUpdate("u2")).thenReturn(Optional.empty());
        when(badgeService.checkAndUnlockBadges(eq("u2"), any())).thenReturn(List.of());

        DepositResponse r = service.processDeposit("u2", new DepositRequest(null, BARCODE, "Alu"));

        assertEquals(50, r.totalPoints());
    }

    @Test
    void getProfile_includesDisplayedAndLongestStreak() {
        UserStats s = new UserStats();
        s.setUserId("u1");
        s.setLongestStreak(12);
        Badge b = new Badge();
        b.setId("eco-family");
        when(statsRepo.findById("u1")).thenReturn(Optional.of(s));
        when(userBadgeRepo.findBadgeIdsByUserId("u1")).thenReturn(Set.of());
        when(badgeRepo.findAll()).thenReturn(List.of(b));
        when(streakService.currentStreakFor(s)).thenReturn(0);

        UserProfileResponse p = service.getProfile("u1");

        assertEquals(0, p.currentStreak());
        assertEquals(12, p.longestStreak());
        assertEquals(1, p.badges().size());
    }

    @Test
    void getLeaderboard_ordersByPointsAndLimits() {
        UserStats a = new UserStats(); a.setUserId("a"); a.setTotalPoints(10);
        UserStats b = new UserStats(); b.setUserId("b"); b.setTotalPoints(99);
        when(statsRepo.findAll()).thenReturn(List.of(a, b));
        when(statsRepo.findById("b")).thenReturn(Optional.of(b));
        when(userBadgeRepo.findBadgeIdsByUserId("b")).thenReturn(Set.of());
        when(badgeRepo.findAll()).thenReturn(List.of());

        List<UserProfileResponse> top = service.getLeaderboard(1);

        assertEquals(1, top.size());
        assertEquals("b", top.get(0).userId());
    }
}
