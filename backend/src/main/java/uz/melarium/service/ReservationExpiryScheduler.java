package uz.melarium.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiryScheduler {
    private final InvestmentService investmentService;

    public ReservationExpiryScheduler(InvestmentService investmentService) { this.investmentService = investmentService; }

    @Scheduled(fixedDelayString = "${app.investment.expiry-sweep-ms:60000}")
    public void releaseExpiredReservations() {
        investmentService.expireReservations();
    }
}
