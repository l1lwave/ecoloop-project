package com.ecoapp.reward.service;

import com.ecoapp.reward.dto.MonthlyReturnDto;
import com.ecoapp.reward.dto.ReturnsSummaryResponse;
import com.ecoapp.reward.model.DepositRecord;
import com.ecoapp.reward.repository.DepositRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Monthly return statistics for the caller (ADR 0003). Months are calendar months in the zone of the
 * injected Clock, which ClockConfig sets to Europe/Amsterdam, the same zone as the streak feature.
 */
@Service
public class ReturnsService {

    private static final int MONTHS_PER_YEAR = 12;
    private static final BigDecimal GRAMS_PER_KG = BigDecimal.valueOf(1000);

    private final DepositRecordRepository depositRepo;
    private final Clock clock;

    public ReturnsService(DepositRecordRepository depositRepo, Clock clock) {
        this.depositRepo = depositRepo;
        this.clock = clock;
    }

    /**
     * Returns exactly 12 month entries for the given year. Each month is one query with Amsterdam
     * month boundaries; the userId must come from the verified gateway header, never from the client.
     */
    @Transactional(readOnly = true)
    public ReturnsSummaryResponse getSummary(String userId, int year) {
        ZoneId zone = clock.getZone();
        List<MonthlyReturnDto> months = new ArrayList<>(MONTHS_PER_YEAR);
        int totalItems = 0;
        BigDecimal totalDeposit = BigDecimal.ZERO;
        BigDecimal totalPlastic = BigDecimal.ZERO;

        for (int month = 1; month <= MONTHS_PER_YEAR; month++) {
            YearMonth yearMonth = YearMonth.of(year, month);
            // First instant of the month and of the next month, in the Amsterdam zone, as local date-times
            // to match deposited_at.
            LocalDateTime start = yearMonth.atDay(1).atStartOfDay(zone).toLocalDateTime();
            LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toLocalDateTime();

            MonthlyReturnDto entry = summarize(month, depositRepo.findReturnsBetween(userId, start, end));
            months.add(entry);
            totalItems += entry.itemsReturned();
            totalDeposit = totalDeposit.add(BigDecimal.valueOf(entry.depositEur()));
            totalPlastic = totalPlastic.add(BigDecimal.valueOf(entry.plasticKg()));
        }

        return new ReturnsSummaryResponse(
                year,
                months,
                totalItems,
                totalDeposit.setScale(2, RoundingMode.HALF_UP).doubleValue(),
                totalPlastic.setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    private MonthlyReturnDto summarize(int month, List<DepositRecord> rows) {
        BigDecimal deposit = BigDecimal.ZERO;
        for (DepositRecord row : rows) {
            // Rows from before the deposit check have a NULL deposit_value. They count as returned items,
            // but no money is added for them, because their value is unknown and must not be guessed.
            if (row.getDepositValue() != null) {
                deposit = deposit.add(BigDecimal.valueOf(row.getDepositValue()));
            }
        }
        int items = rows.size();
        // Plastic weight per item uses the same constant as the RewardService statistics (grams per item).
        BigDecimal plasticKg = BigDecimal.valueOf(items)
                .multiply(BigDecimal.valueOf(RewardService.PET_WEIGHT_GRAMS))
                .divide(GRAMS_PER_KG, 2, RoundingMode.HALF_UP);
        return new MonthlyReturnDto(
                month,
                items,
                deposit.setScale(2, RoundingMode.HALF_UP).doubleValue(),
                plasticKg.doubleValue());
    }
}
