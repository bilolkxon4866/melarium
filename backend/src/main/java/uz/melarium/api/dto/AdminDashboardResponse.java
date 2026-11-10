package uz.melarium.api.dto;

public record AdminDashboardResponse(
        long totalUsers,
        long activeUsers,
        long investors,
        long beekeepers,
        long suspendedUsers,
        long activeListings,
        long pendingListings,
        long pendingOrders,
        long paymentReviewOrders,
        long activeInvestments,
        long pendingPayments,
        long successfulPayments,
        long submittedHarvestReports
) { }
