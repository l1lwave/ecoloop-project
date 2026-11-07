package com.ecoapp.reward.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClockConfigTest {
    @Test
    void clock_usesAmsterdamZone() {
        Clock clock = new ClockConfig().clock();
        assertEquals("Europe/Amsterdam", clock.getZone().getId());
    }
}
