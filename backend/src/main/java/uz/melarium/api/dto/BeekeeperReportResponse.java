package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BeekeeperReportResponse(
        long apiaryCount,
        long colonyCount,
        long activeListings,
        long pendingListings,
        BigDecimal harvestKg,
        BigDecimal expensesUzs,
        LocalDate from,
        LocalDate to
) { }
