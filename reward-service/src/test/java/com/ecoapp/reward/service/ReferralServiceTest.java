package com.ecoapp.reward.service;

import com.ecoapp.reward.dto.ReferralMeResponse;
import com.ecoapp.reward.exception.InvalidReferralException;
import com.ecoapp.reward.model.Referral;
import com.ecoapp.reward.model.ReferralCode;
import com.ecoapp.reward.model.ReferralStatus;
import com.ecoapp.reward.model.UserStats;
import com.ecoapp.reward.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReferralServiceTest {

    private final ReferralCodeRepository codeRepo = mock(ReferralCodeRepository.class);
    private final ReferralRepository referralRepo = mock(ReferralRepository.class);
    private final UserStatsRepository statsRepo = mock(UserStatsRepository.class);
    private final DepositRecordRepository depositRepo = mock(DepositRecordRepository.class);
    private final UserBadgeRepository userBadgeRepo = mock(UserBadgeRepository.class);
    private final BadgeService badgeService = mock(BadgeService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T10:00:00Z"), ZoneId.of("Europe/Amsterdam"));
    private ReferralService service;

    @BeforeEach
    void setUp() {
        service = new ReferralService(codeRepo, referralRepo, statsRepo, depositRepo, userBadgeRepo,
                badgeService, clock, new Random(42));
    }

    private ReferralCode code(String userId, String code) {
        ReferralCode c = new ReferralCode();
        c.setUserId(userId);
        c.setCode(code);
        return c;
    }

    private Referral pending(long id, String invited, String referrer) {
        Referral r = new Referral();
        r.setInvitedUserId(invited);
        r.setReferrerUserId(referrer);
        try {
            var f = Referral.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(r, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return r;
    }

    @Test
    void getOrCreateCode_returnsExistingCode() {
        when(codeRepo.findByUserId("u1")).thenReturn(Optional.of(code("u1", "K7M2QX9P")));

        assertEquals("K7M2QX9P", service.getOrCreateCode("u1"));
        verify(codeRepo, never()).insertIfAbsent(anyString(), anyString());
    }

    @Test
    void getOrCreateCode_generatesEightCharsWithoutAmbiguousCharacters() {
        when(codeRepo.findByUserId("u1")).thenReturn(Optional.empty());
        when(codeRepo.insertIfAbsent(eq("u1"), anyString())).thenReturn(1);

        for (int i = 0; i < 200; i++) {
            String c = service.getOrCreateCode("u1");
            assertTrue(Pattern.matches("[A-HJ-KM-NP-Z2-9]{8}", c), c);
        }
    }

    @Test
    void getOrCreateCode_retriesOnCodeCollision() {
        when(codeRepo.findByUserId("u1")).thenReturn(Optional.empty());
        when(codeRepo.insertIfAbsent(eq("u1"), anyString())).thenReturn(0, 0, 1);

        String c = service.getOrCreateCode("u1");

        assertEquals(8, c.length());
        verify(codeRepo, times(3)).insertIfAbsent(eq("u1"), anyString());
    }

    @Test
    void getOrCreateCode_returnsCodeCreatedByConcurrentRequest() {
        when(codeRepo.findByUserId("u1")).thenReturn(Optional.empty(), Optional.of(code("u1", "AAAAAAAA")));
        when(codeRepo.insertIfAbsent(eq("u1"), anyString())).thenReturn(0);

        assertEquals("AAAAAAAA", service.getOrCreateCode("u1"));
    }

    @Test
    void getOrCreateCode_failsAfterTooManyCollisions() {
        when(codeRepo.findByUserId("u1")).thenReturn(Optional.empty());
        when(codeRepo.insertIfAbsent(eq("u1"), anyString())).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.getOrCreateCode("u1"));
    }

    @Test
    void getSummary_returnsCountsAndBadgeFlagWithoutInviteeIds() {
        when(codeRepo.findByUserId("u1")).thenReturn(Optional.of(code("u1", "K7M2QX9P")));
        when(referralRepo.countByReferrerUserIdAndStatus("u1", ReferralStatus.CREDITED)).thenReturn(3L);
        when(referralRepo.countByReferrerUserIdAndStatus("u1", ReferralStatus.PENDING)).thenReturn(1L);
        when(userBadgeRepo.findBadgeIdsByUserId("u1")).thenReturn(Set.of("eco-ambassador"));

        ReferralMeResponse r = service.getSummary("u1");

        assertEquals(new ReferralMeResponse("K7M2QX9P", 3, 1, true), r);
    }

    @Test
    void claim_createsPendingReferral() {
        when(codeRepo.findByCode("K7M2QX9P")).thenReturn(Optional.of(code("ref", "K7M2QX9P")));
        when(depositRepo.countByUserId("inv")).thenReturn(0L);
        when(referralRepo.insertPendingIfAbsent(eq("inv"), eq("ref"), any())).thenReturn(1);

        assertDoesNotThrow(() -> service.claim("inv", "K7M2QX9P"));
        verifyNoInteractions(statsRepo);
    }

    @Test
    void claim_unknownCodeRejected() {
        when(codeRepo.findByCode("K7M2QX9P")).thenReturn(Optional.empty());

        assertThrows(InvalidReferralException.class, () -> service.claim("inv", "K7M2QX9P"));
        verify(referralRepo, never()).insertPendingIfAbsent(any(), any(), any());
    }

    @Test
    void claim_selfReferralRejected() {
        when(codeRepo.findByCode("K7M2QX9P")).thenReturn(Optional.of(code("u1", "K7M2QX9P")));

        assertThrows(InvalidReferralException.class, () -> service.claim("u1", "K7M2QX9P"));
        verify(referralRepo, never()).insertPendingIfAbsent(any(), any(), any());
    }

    @Test
    void claim_afterDepositRejected() {
        when(codeRepo.findByCode("K7M2QX9P")).thenReturn(Optional.of(code("ref", "K7M2QX9P")));
        when(depositRepo.countByUserId("inv")).thenReturn(1L);

        assertThrows(InvalidReferralException.class, () -> service.claim("inv", "K7M2QX9P"));
        verify(referralRepo, never()).insertPendingIfAbsent(any(), any(), any());
    }

    @Test
    void claim_repeatClaimRejected() {
        when(codeRepo.findByCode("K7M2QX9P")).thenReturn(Optional.of(code("ref", "K7M2QX9P")));
        when(depositRepo.countByUserId("inv")).thenReturn(0L);
        when(referralRepo.insertPendingIfAbsent(eq("inv"), eq("ref"), any())).thenReturn(0);

        assertThrows(InvalidReferralException.class, () -> service.claim("inv", "K7M2QX9P"));
    }

    @Test
    void lockParticipants_locksLowerUserIdFirst() {
        when(referralRepo.findByInvitedUserIdAndStatus("b", ReferralStatus.PENDING))
                .thenReturn(Optional.of(pending(1, "b", "a")));

        service.lockParticipants("b");

        var order = inOrder(statsRepo);
        order.verify(statsRepo).findByIdForUpdate("a");
        order.verify(statsRepo).findByIdForUpdate("b");
    }

    @Test
    void lockParticipants_lowerInviteeLockedFirst() {
        when(referralRepo.findByInvitedUserIdAndStatus("a", ReferralStatus.PENDING))
                .thenReturn(Optional.of(pending(1, "a", "b")));

        service.lockParticipants("a");

        var order = inOrder(statsRepo);
        order.verify(statsRepo).findByIdForUpdate("a");
        order.verify(statsRepo).findByIdForUpdate("b");
    }

    @Test
    void lockParticipants_noPendingReferralDoesNothing() {
        when(referralRepo.findByInvitedUserIdAndStatus("a", ReferralStatus.PENDING)).thenReturn(Optional.empty());

        service.lockParticipants("a");

        verifyNoInteractions(statsRepo);
    }

    @Test
    void creditOnFirstDeposit_paysBothSidesAndChecksReferrerBadges() {
        UserStats invitee = new UserStats();
        invitee.setUserId("inv");
        invitee.setTotalPoints(50);
        UserStats referrer = new UserStats();
        referrer.setUserId("ref");
        referrer.setTotalPoints(100);
        when(referralRepo.findByInvitedUserIdAndStatus("inv", ReferralStatus.PENDING))
                .thenReturn(Optional.of(pending(7, "inv", "ref")));
        when(referralRepo.markCredited(eq(7L), any())).thenReturn(1);
        when(statsRepo.findByIdForUpdate("ref")).thenReturn(Optional.of(referrer));

        assertTrue(service.creditOnFirstDeposit("inv", invitee));

        assertEquals(100, invitee.getTotalPoints());
        assertEquals(150, referrer.getTotalPoints());
        verify(statsRepo).save(referrer);
        verify(badgeService).checkAndUnlockBadges("ref", referrer);
    }

    @Test
    void creditOnFirstDeposit_noPendingReferralPaysNothing() {
        UserStats invitee = new UserStats();
        when(referralRepo.findByInvitedUserIdAndStatus("inv", ReferralStatus.PENDING))
                .thenReturn(Optional.empty());

        assertFalse(service.creditOnFirstDeposit("inv", invitee));

        assertEquals(0, invitee.getTotalPoints());
        verifyNoInteractions(statsRepo, badgeService);
    }

    @Test
    void creditOnFirstDeposit_lostRaceDoesNotPayTwice() {
        UserStats invitee = new UserStats();
        when(referralRepo.findByInvitedUserIdAndStatus("inv", ReferralStatus.PENDING))
                .thenReturn(Optional.of(pending(7, "inv", "ref")));
        when(referralRepo.markCredited(eq(7L), any())).thenReturn(0);

        assertFalse(service.creditOnFirstDeposit("inv", invitee));

        assertEquals(0, invitee.getTotalPoints());
        verifyNoInteractions(statsRepo, badgeService);
    }
}
