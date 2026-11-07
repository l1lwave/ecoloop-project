package com.ecoapp.reward.repository;

import com.ecoapp.reward.model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, String> {}
