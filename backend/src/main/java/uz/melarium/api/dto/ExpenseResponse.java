package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import uz.melarium.domain.Expense;

public record ExpenseResponse(UUID id, UUID apiaryId, UUID colonyId, UUID reportedBy, String category, BigDecimal amount, String currency, LocalDate spentOn, String note, Instant createdAt) {
    public static ExpenseResponse from(Expense item) {
        return new ExpenseResponse(item.getId(), item.getApiaryId(), item.getColonyId(), item.getReportedBy(), item.getCategory(), item.getAmount(), item.getCurrency(), item.getSpentOn(), item.getNote(), item.getCreatedAt());
    }
}
