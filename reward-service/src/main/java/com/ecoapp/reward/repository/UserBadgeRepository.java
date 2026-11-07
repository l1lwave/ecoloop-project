package com.ecoapp.reward.repository;

import com.ecoapp.reward.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserId(String userId);

    default Set<String> findBadgeIdsByUserId(String userId) {
        return findByUserId(userId).stream()
                .map(UserBadge::getBadgeId)
                .collect(Collectors.toSet());
    }
}
