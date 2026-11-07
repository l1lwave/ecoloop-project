package com.ecoapp.scan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /** Scan timestamps are stored in Dutch local time, matching the reward-service zone. Tests inject a fixed Clock. */
    public static final ZoneId SCAN_ZONE = ZoneId.of("Europe/Amsterdam");

    @Bean
    public Clock clock() {
        return Clock.system(SCAN_ZONE);
    }
}
