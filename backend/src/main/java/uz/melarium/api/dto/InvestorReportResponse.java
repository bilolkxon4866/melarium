package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record InvestorReportResponse(
        long pendingOrders,
        long activeInvestments,
        long cancelledOrders,
        BigDecimal pendingOrderAmount,
        BigDecimal activePrincipalAmount,
        List<InvestmentResponse> investments,
        List<HarvestResponse> apiaryReports,
        List<ExpenseResponse> apiaryExpenses,
        String financialDisclaimer
) { }
