package com.ecoapp.reward.service;

import com.ecoapp.reward.model.Badge;
import com.ecoapp.reward.model.ReferralStatus;
import com.ecoapp.reward.model.UserBadge;
import com.ecoapp.reward.model.UserStats;
import com.ecoapp.reward.repository.BadgeRepository;
import com.ecoapp.reward.repository.DepositRecordRepository;
import com.ecoapp.reward.repository.ReferralRepository;
import com.ecoapp.reward.repository.UserBadgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Badge unlocking, shared by the deposit and check-in paths. Badges are never revoked. */
@Service
public class BadgeService {

    private final BadgeRepository badgeRepo;
    private final UserBadgeRepository userBadgeRepo;
    private final DepositRecordRepository depositRepo;
    private final ReferralRepository referralRepo;

    public BadgeService(BadgeRepository badgeRepo, UserBadgeRepository userBadgeRepo,
                        DepositRecordRepository depositRepo, ReferralRepository referralRepo) {
        this.badgeRepo = badgeRepo;
        this.userBadgeRepo = userBadgeRepo;
        this.depositRepo = depositRepo;
        this.referralRepo = referralRepo;
    }

    /** Unlocks every badge whose condition is met and returns the ids that were newly unlocked. */
    @Transactional
    public List<String> checkAndUnlockBadges(String userId, UserStats stats) {
        Set<String> already = userBadgeRepo.findBadgeIdsByUserId(userId);
        long depositCount = depositRepo.countByUserId(userId);
        List<String> newlyUnlocked = new ArrayList<>();

        for (Badge badge : badgeRepo.findAll()) {
            if (already.contains(badge.getId())) continue;

            boolean earned = (badge.getRequiredPoints() > 0 && stats.getTotalPoints() >= badge.getRequiredPoints())
                    || (badge.getRequiredDeposits() > 0 && depositCount >= badge.getRequiredDeposits())
                    || (badge.getRequiredLocations() > 0 && stats.getLocationsVisited() >= badge.getRequiredLocations())
                    || (badge.getRequiredStreakDays() > 0 && stats.getCurrentStreak() >= badge.getRequiredStreakDays())
                    || (badge.getRequiredReferrals() > 0 && creditedReferrals(userId) >= badge.getRequiredReferrals());

            if (earned) {
                UserBadge ub = new UserBadge();
                ub.setUserId(userId);
                ub.setBadgeId(badge.getId());
                userBadgeRepo.save(ub);
                newlyUnlocked.add(badge.getId());
            }
        }
        return newlyUnlocked;
    }

    private long creditedReferrals(String userId) {
        return referralRepo.countByReferrerUserIdAndStatus(userId, ReferralStatus.CREDITED);
    }
}
