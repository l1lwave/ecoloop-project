package com.ecoapp.reward.controller;

import com.ecoapp.reward.exception.GlobalExceptionHandler;
import com.ecoapp.reward.repository.DepositRecordRepository;
import com.ecoapp.reward.security.RequireUserIdFilter;
import com.ecoapp.reward.service.ReturnsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web layer is real (controller, binding, GlobalExceptionHandler, RequireUserIdFilter); the repository is mocked. */
class ReturnsApiTest {

    private final DepositRecordRepository depositRepo = mock(DepositRecordRepository.class);
    private final ReturnsService service = new ReturnsService(depositRepo,
            Clock.fixed(Instant.parse("2026-10-27T10:00:00Z"), ZoneId.of("Europe/Amsterdam")));

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(depositRepo.findReturnsBetween(any(), any(), any())).thenReturn(List.of());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReturnsController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequireUserIdFilter())
                .build();
    }

    @Test
    void shouldReturn200WithTwelveMonthsForValidYear() throws Exception {
        mockMvc.perform(get("/api/rewards/returns/summary").param("year", "2026").header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.months.length()").value(12))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void shouldReturn400InvalidRequestWhenYearIsOutOfRange() throws Exception {
        for (String year : new String[]{"1999", "2101", "20x6", "26", "99999"}) {
            mockMvc.perform(get("/api/rewards/returns/summary").param("year", year).header("X-User-Id", "u1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        }
        verifyNoInteractions(depositRepo);
    }

    @Test
    void shouldReturn400InvalidRequestWhenYearIsMissing() throws Exception {
        mockMvc.perform(get("/api/rewards/returns/summary").header("X-User-Id", "u1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        verifyNoInteractions(depositRepo);
    }

    @Test
    void shouldReturn401WhenUserIdHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/rewards/returns/summary").param("year", "2026"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(depositRepo);
    }
}
