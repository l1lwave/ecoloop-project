package com.ecoapp.reward.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /** Day boundary for streaks is Dutch local midnight (see ADR 0001). Tests inject a fixed Clock instead. */
    public static final ZoneId STREAK_ZONE = ZoneId.of("Europe/Amsterdam");

    @Bean
    public Clock clock() {
        return Clock.system(STREAK_ZONE);
    }
}
