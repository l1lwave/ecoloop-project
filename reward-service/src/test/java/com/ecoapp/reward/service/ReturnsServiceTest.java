package com.ecoapp.reward.service;

import com.ecoapp.reward.dto.MonthlyReturnDto;
import com.ecoapp.reward.dto.ReturnsSummaryResponse;
import com.ecoapp.reward.model.DepositRecord;
import com.ecoapp.reward.repository.DepositRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnsServiceTest {

    private static final ZoneId AMS = ZoneId.of("Europe/Amsterdam");

    @Mock DepositRecordRepository depositRepo;

    private ReturnsService service;

    @BeforeEach
    void setUp() {
        service = new ReturnsService(depositRepo,
                Clock.fixed(Instant.parse("2026-10-27T10:00:00Z"), AMS));
    }

    /** Stored deposited_at is Amsterdam local time, so an instant is converted before it is stored. */
    private static DepositRecord depositAt(Instant instant, Double depositValue) {
        DepositRecord r = new DepositRecord();
        r.setUserId("u1");
        r.setDepositValue(depositValue);
        ReflectionTestUtils.setField(r, "depositedAt", LocalDateTime.ofInstant(instant, AMS));
        return r;
    }

    /** Fake query: returns only the stored rows whose deposited_at lies in the requested [start, end). */
    private void repositoryHolds(List<DepositRecord> rows) {
        when(depositRepo.findReturnsBetween(eq("u1"), any(), any())).thenAnswer(inv -> {
            LocalDateTime start = inv.getArgument(1);
            LocalDateTime end = inv.getArgument(2);
            return rows.stream()
                    .filter(r -> !r.getDepositedAt().isBefore(start) && r.getDepositedAt().isBefore(end))
                    .toList();
        });
    }

    private static MonthlyReturnDto month(ReturnsSummaryResponse r, int month) {
        return r.months().get(month - 1);
    }

    @Test
    void getSummary_withoutDeposits_returnsTwelveZeroMonths() {
        repositoryHolds(List.of());

        ReturnsSummaryResponse r = service.getSummary("u1", 2026);

        assertEquals(2026, r.year());
        assertEquals(12, r.months().size());
        for (int m = 1; m <= 12; m++) {
            assertEquals(m, month(r, m).month());
            assertEquals(0, month(r, m).itemsReturned());
            assertEquals(0.0, month(r, m).depositEur());
            assertEquals(0.0, month(r, m).plasticKg());
        }
        assertEquals(0, r.totalItems());
        assertEquals(0.0, r.totalDepositEur());
        assertEquals(0.0, r.totalPlasticKg());
    }

    @Test
    void getSummary_queriesEachMonthWithAmsterdamBoundaries() {
        repositoryHolds(List.of());

        service.getSummary("u1", 2026);

        verify(depositRepo).findReturnsBetween("u1",
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0));
        verify(depositRepo).findReturnsBetween("u1",
                LocalDateTime.of(2026, 12, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0));
        verify(depositRepo, times(12)).findReturnsBetween(eq("u1"), any(), any());
    }

    @Test
    void getSummary_depositAt2330UtcOnLastDayOfMonth_countsInNextMonth() {
        // 31 Oct 2026 23:30 UTC is 1 Nov 2026 00:30 in Amsterdam (CET, UTC+1 after DST ends).
        repositoryHolds(List.of(depositAt(Instant.parse("2026-10-31T23:30:00Z"), 0.15)));

        ReturnsSummaryResponse r = service.getSummary("u1", 2026);

        assertEquals(0, month(r, 10).itemsReturned());
        assertEquals(1, month(r, 11).itemsReturned());
        assertEquals(0.15, month(r, 11).depositEur());
    }

    @Test
    void getSummary_depositAt2230UtcOnLastDayOfMonth_staysInSameMonth() {
        // 31 Oct 2026 22:30 UTC is 31 Oct 2026 23:30 in Amsterdam, still October.
        repositoryHolds(List.of(depositAt(Instant.parse("2026-10-31T22:30:00Z"), 0.15)));

        ReturnsSummaryResponse r = service.getSummary("u1", 2026);

        assertEquals(1, month(r, 10).itemsReturned());
        assertEquals(0, month(r, 11).itemsReturned());
    }

    @Test
    void getSummary_nullDepositValueCountsItemButAddsNoMoney() {
        repositoryHolds(List.of(
                depositAt(Instant.parse("2026-03-10T10:00:00Z"), null),
                depositAt(Instant.parse("2026-03-11T10:00:00Z"), 0.15)));

        ReturnsSummaryResponse r = service.getSummary("u1", 2026);

        assertEquals(2, month(r, 3).itemsReturned());
        assertEquals(0.15, month(r, 3).depositEur());
        assertEquals(2, r.totalItems());
        assertEquals(0.15, r.totalDepositEur());
    }

    @Test
    void getSummary_otherUsersRowsAreNeverRequested() {
        repositoryHolds(List.of());

        service.getSummary("u1", 2026);

        verify(depositRepo, never()).findReturnsBetween(eq("u2"), any(), any());
    }

    @Test
    void getSummary_plasticIsThirtyGramsPerItemRoundedToTwoDecimals() {
        repositoryHolds(List.of(
                depositAt(Instant.parse("2026-01-05T10:00:00Z"), 0.15),
                depositAt(Instant.parse("2026-01-06T10:00:00Z"), 0.15),
                depositAt(Instant.parse("2026-01-07T10:00:00Z"), 0.15),
                depositAt(Instant.parse("2026-01-08T10:00:00Z"), 0.15)));

        ReturnsSummaryResponse r = service.getSummary("u1", 2026);

        assertEquals(4, month(r, 1).itemsReturned());
        assertEquals(0.60, month(r, 1).depositEur());
        assertEquals(0.12, month(r, 1).plasticKg());
        assertEquals(0.12, r.totalPlasticKg());
    }

    @Test
    void getSummary_depositRoundsHalfUpToTwoDecimals() {
        repositoryHolds(List.of(depositAt(Instant.parse("2026-02-01T10:00:00Z"), 0.155)));

        ReturnsSummaryResponse r = service.getSummary("u1", 2026);

        assertEquals(0.16, month(r, 2).depositEur());
        assertEquals(0.16, r.totalDepositEur());
    }

    @Test
    void getSummary_totalsAreSumsOfMonthEntries() {
        repositoryHolds(List.of(
                depositAt(Instant.parse("2026-01-05T10:00:00Z"), 0.15),
                depositAt(Instant.parse("2026-06-05T10:00:00Z"), 0.15),
                depositAt(Instant.parse("2026-06-06T10:00:00Z"), null),
                depositAt(Instant.parse("2026-12-24T10:00:00Z"), 0.15)));

        ReturnsSummaryResponse r = service.getSummary("u1", 2026);

        // Three rows carry 0.15 each; the NULL row counts as an item but adds no money.
        assertEquals(4, r.totalItems());
        assertEquals(0.45, r.totalDepositEur());
        assertEquals(0.12, r.totalPlasticKg());
    }
}
