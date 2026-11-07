package com.ecoapp.reward.service;

import com.ecoapp.reward.client.ScanDepositClient;
import com.ecoapp.reward.dto.*;
import com.ecoapp.reward.exception.DepositRefusedException;
import com.ecoapp.reward.model.*;
import com.ecoapp.reward.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;

@Service
public class RewardService {

    private static final int POINTS_PER_PET_BOTTLE = 50;
    private static final int POINTS_PER_ALU_CAN    = 50;
    // Average PET bottle weight: 30g; average water saving per recycled bottle: 3700ml
    // Package-private so ReturnsService uses the same per-item weight.
    static final int PET_WEIGHT_GRAMS               = 30;
    private static final long WATER_SAVED_ML_PER_ITEM = 3700;

    private final UserStatsRepository statsRepo;
    private final BadgeRepository badgeRepo;
    private final UserBadgeRepository userBadgeRepo;
    private final DepositRecordRepository depositRepo;
    private final BadgeService badgeService;
    private final StreakService streakService;
    private final ReferralService referralService;
    private final ScanDepositClient scanDepositClient;
    private final TransactionTemplate tx;

    public RewardService(UserStatsRepository statsRepo, BadgeRepository badgeRepo,
                         UserBadgeRepository userBadgeRepo, DepositRecordRepository depositRepo,
                         BadgeService badgeService, StreakService streakService,
                         ReferralService referralService, ScanDepositClient scanDepositClient,
                         TransactionTemplate tx) {
        this.scanDepositClient = scanDepositClient;
        this.tx = tx;
        this.badgeService = badgeService;
        this.streakService = streakService;
        this.referralService = referralService;
        this.statsRepo = statsRepo;
        this.badgeRepo = badgeRepo;
        this.userBadgeRepo = userBadgeRepo;
        this.depositRepo = depositRepo;
    }

    /**
     * Not @Transactional on purpose: the scan-service call happens first, outside any transaction, so no
     * database row lock is held during network I/O. Only the writes run inside a TransactionTemplate.
     * The deposit value stored comes from scan-service, never from the client.
     */
    public DepositResponse processDeposit(String userId, DepositRequest request) {
        ScanDepositClient.DepositCheck check = scanDepositClient.check(request.barcode());
        if (!check.eligible()) {
            throw DepositRefusedException.notReturnable();
        }
        return tx.execute(status -> creditDeposit(userId, request, check.depositValue()));
    }

    private DepositResponse creditDeposit(String userId, DepositRequest request, double depositValue) {
        // Lock both sides of a pending referral first, lower user id first, so deposits of two users
        // who are linked by a referral cannot deadlock each other.
        referralService.lockParticipants(userId);
        // Locking read: serializes this deposit with a concurrent check-in of the same user.
        UserStats stats = statsRepo.findByIdForUpdate(userId)
                .orElseGet(() -> createNewStats(userId));

        int points = "Alu".equalsIgnoreCase(request.materialType())
                ? POINTS_PER_ALU_CAN : POINTS_PER_PET_BOTTLE;

        stats.setTotalPoints(stats.getTotalPoints() + points);
        stats.setEcoChecks(stats.getEcoChecks() + 1);
        stats.setPlasticRecycledGrams(stats.getPlasticRecycledGrams() + PET_WEIGHT_GRAMS);
        stats.setWaterSavedMl(stats.getWaterSavedMl() + WATER_SAVED_ML_PER_ITEM);
        stats.setWeeklyPlasticGrams(stats.getWeeklyPlasticGrams() + PET_WEIGHT_GRAMS);
        // A deposit counts as activity for today; same transaction as the deposit itself.
        streakService.recordActivity(userId, stats);
        // Pays the referral bonus (once) in this same transaction; adds points to the invitee's stats.
        referralService.creditOnFirstDeposit(userId, stats);
        statsRepo.save(stats);

        DepositRecord record = new DepositRecord();
        record.setUserId(userId);
        record.setLocationId(request.locationId());
        record.setBarcode(request.barcode());
        record.setMaterialType(request.materialType());
        record.setPointsEarned(points);
        record.setDepositValue(depositValue);
        depositRepo.save(record);

        List<String> newBadges = badgeService.checkAndUnlockBadges(userId, stats);

        return new DepositResponse(
                points,
                stats.getTotalPoints(),
                stats.getEcoSledLevel(),
                newBadges,
                stats.getPlasticRecycledGrams() / 1000.0,
                stats.getWaterSavedMl() / 1000
        );
    }

    public UserProfileResponse getProfile(String userId) {
        UserStats stats = statsRepo.findById(userId)
                .orElseGet(() -> createNewStats(userId));

        Set<String> earnedBadgeIds = userBadgeRepo.findBadgeIdsByUserId(userId);
        List<BadgeDto> badgeDtos = badgeRepo.findAll().stream()
                .map(b -> new BadgeDto(b.getId(), b.getName(), b.getDescription(),
                        earnedBadgeIds.contains(b.getId())))
                .toList();

        return new UserProfileResponse(
                userId,
                stats.getTotalPoints(),
                stats.getEcoSledLevel(),
                stats.getPlasticRecycledGrams() / 1000.0,
                stats.getWaterSavedMl() / 1000,
                stats.getLocationsVisited(),
                stats.getEcoChecks(),
                stats.getWeeklyPlasticGrams() / 1000.0,
                badgeDtos,
                streakService.currentStreakFor(stats),
                stats.getLongestStreak()
        );
    }

    public List<UserProfileResponse> getLeaderboard(int limit) {
        return statsRepo.findAll().stream()
                .sorted((a, b) -> b.getTotalPoints() - a.getTotalPoints())
                .limit(limit)
                .map(s -> getProfile(s.getUserId()))
                .toList();
    }

    private UserStats createNewStats(String userId) {
        UserStats s = new UserStats();
        s.setUserId(userId);
        return s;
    }
}
