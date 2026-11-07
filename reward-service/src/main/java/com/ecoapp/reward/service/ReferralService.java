package com.ecoapp.reward.service;

import com.ecoapp.reward.dto.ReferralMeResponse;
import com.ecoapp.reward.exception.InvalidReferralException;
import com.ecoapp.reward.model.Referral;
import com.ecoapp.reward.model.ReferralCode;
import com.ecoapp.reward.model.ReferralStatus;
import com.ecoapp.reward.model.UserStats;
import com.ecoapp.reward.repository.DepositRecordRepository;
import com.ecoapp.reward.repository.ReferralCodeRepository;
import com.ecoapp.reward.repository.ReferralRepository;
import com.ecoapp.reward.repository.UserBadgeRepository;
import com.ecoapp.reward.repository.UserStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/** Referral codes, claims and the first-deposit payout (ADR 0002). */
@Service
public class ReferralService {

    public static final String AMBASSADOR_BADGE_ID = "eco-ambassador";
    static final int BONUS_POINTS = 50;
    static final int CODE_LENGTH = 8;
    /** No 0/O, 1/I/L. Keep in sync with the pattern in ClaimReferralRequest. */
    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int MAX_CODE_ATTEMPTS = 10;

    private final ReferralCodeRepository codeRepo;
    private final ReferralRepository referralRepo;
    private final UserStatsRepository statsRepo;
    private final DepositRecordRepository depositRepo;
    private final UserBadgeRepository userBadgeRepo;
    private final BadgeService badgeService;
    private final Clock clock;
    private final Random random;

    @Autowired
    public ReferralService(ReferralCodeRepository codeRepo, ReferralRepository referralRepo,
                           UserStatsRepository statsRepo, DepositRecordRepository depositRepo,
                           UserBadgeRepository userBadgeRepo, BadgeService badgeService, Clock clock) {
        this(codeRepo, referralRepo, statsRepo, depositRepo, userBadgeRepo, badgeService, clock,
                new SecureRandom());
    }

    /** Visible for tests: allows a deterministic random source. */
    ReferralService(ReferralCodeRepository codeRepo, ReferralRepository referralRepo,
                    UserStatsRepository statsRepo, DepositRecordRepository depositRepo,
                    UserBadgeRepository userBadgeRepo, BadgeService badgeService, Clock clock,
                    Random random) {
        this.codeRepo = codeRepo;
        this.referralRepo = referralRepo;
        this.statsRepo = statsRepo;
        this.depositRepo = depositRepo;
        this.userBadgeRepo = userBadgeRepo;
        this.badgeService = badgeService;
        this.clock = clock;
        this.random = random;
    }

    /** Returns the user's code, creating it on first call. Repeated calls return the same code. */
    @Transactional
    public String getOrCreateCode(String userId) {
        Optional<ReferralCode> existing = codeRepo.findByUserId(userId);
        if (existing.isPresent()) return existing.get().getCode();

        for (int i = 0; i < MAX_CODE_ATTEMPTS; i++) {
            String candidate = randomCode();
            if (codeRepo.insertIfAbsent(userId, candidate) == 1) return candidate;
            // 0 rows: either a concurrent request created this user's code, or the code collided.
            Optional<ReferralCode> raced = codeRepo.findByUserId(userId);
            if (raced.isPresent()) return raced.get().getCode();
        }
        throw new IllegalStateException("Could not allocate a referral code");
    }

    /** Summary for the caller's own profile. Counts only; invitees are never identified. */
    @Transactional
    public ReferralMeResponse getSummary(String userId) {
        String code = getOrCreateCode(userId);
        long credited = referralRepo.countByReferrerUserIdAndStatus(userId, ReferralStatus.CREDITED);
        long pending = referralRepo.countByReferrerUserIdAndStatus(userId, ReferralStatus.PENDING);
        boolean unlocked = userBadgeRepo.findBadgeIdsByUserId(userId).contains(AMBASSADOR_BADGE_ID);
        return new ReferralMeResponse(code, credited, pending, unlocked);
    }

    /**
     * Creates a pending referral. Every rule violation throws the same InvalidReferralException so the
     * response never reveals which rule failed or whether a code exists. No points are paid here.
     */
    @Transactional
    public void claim(String userId, String code) {
        ReferralCode referrerCode = codeRepo.findByCode(code).orElseThrow(InvalidReferralException::new);
        String referrerId = referrerCode.getUserId();
        if (referrerId.equals(userId)) throw new InvalidReferralException();
        if (depositRepo.countByUserId(userId) > 0) throw new InvalidReferralException();
        // 0 rows means the user already has a referral (unique key on the invited user).
        if (referralRepo.insertPendingIfAbsent(userId, referrerId, LocalDateTime.now(clock)) == 0) {
            throw new InvalidReferralException();
        }
    }

    /**
     * Locks the stats rows of both sides of a pending referral in ascending user-id order. Must be called
     * before the deposit takes its own stats lock, so two users who invited each other cannot deadlock.
     * No-op when the user has no pending referral.
     */
    @Transactional
    public void lockParticipants(String userId) {
        Optional<Referral> pending = referralRepo.findByInvitedUserIdAndStatus(userId, ReferralStatus.PENDING);
        if (pending.isEmpty()) return;
        String referrerId = pending.get().getReferrerUserId();
        String first = userId.compareTo(referrerId) < 0 ? userId : referrerId;
        String second = first.equals(userId) ? referrerId : userId;
        statsRepo.findByIdForUpdate(first);
        statsRepo.findByIdForUpdate(second);
    }

    /**
     * Pays the referral bonus if the invitee has a pending referral. Runs inside the deposit transaction.
     * The invitee's (already locked, unsaved) stats are passed in; the referrer row is locked here. The
     * compare-and-set on the status guarantees a single payout.
     *
     * @return true if a bonus was paid
     */
    @Transactional
    public boolean creditOnFirstDeposit(String userId, UserStats inviteeStats) {
        Optional<Referral> pending = referralRepo.findByInvitedUserIdAndStatus(userId, ReferralStatus.PENDING);
        if (pending.isEmpty()) return false;
        Referral referral = pending.get();
        if (referralRepo.markCredited(referral.getId(), LocalDateTime.now(clock)) != 1) return false;

        String referrerId = referral.getReferrerUserId();
        UserStats referrerStats = statsRepo.findByIdForUpdate(referrerId).orElseGet(() -> {
            UserStats s = new UserStats();
            s.setUserId(referrerId);
            return s;
        });
        referrerStats.setTotalPoints(referrerStats.getTotalPoints() + BONUS_POINTS);
        inviteeStats.setTotalPoints(inviteeStats.getTotalPoints() + BONUS_POINTS);
        statsRepo.save(referrerStats);
        // Invitee stats are saved by the deposit flow after this call.

        // The referrer's badges (eco-ambassador, points badges) are re-evaluated with the new count.
        badgeService.checkAndUnlockBadges(referrerId, referrerStats);
        return true;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
