package uz.melarium.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uz.melarium.api.dto.BeekeeperReportResponse;
import uz.melarium.api.dto.HarvestResponse;
import uz.melarium.api.dto.ExpenseResponse;
import uz.melarium.api.dto.InvestmentResponse;
import uz.melarium.api.dto.InvestorReportResponse;
import uz.melarium.domain.Apiary;
import uz.melarium.domain.Harvest;
import uz.melarium.domain.Investment;
import uz.melarium.domain.ListingStatus;
import uz.melarium.domain.MarketplaceListing;
import uz.melarium.repository.ApiaryRepository;
import uz.melarium.repository.ColonyRepository;
import uz.melarium.repository.ExpenseRepository;
import uz.melarium.repository.HarvestRepository;
import uz.melarium.repository.InvestmentRepository;
import uz.melarium.repository.MarketplaceListingRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportsService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Tashkent");
    private final InvestmentRepository investmentRepository;
    private final MarketplaceListingRepository listingRepository;
    private final HarvestRepository harvestRepository;
    private final ApiaryRepository apiaryRepository;
    private final ColonyRepository colonyRepository;
    private final ExpenseRepository expenseRepository;

    public ReportsService(InvestmentRepository investmentRepository, MarketplaceListingRepository listingRepository,
                         HarvestRepository harvestRepository, ApiaryRepository apiaryRepository,
                         ColonyRepository colonyRepository, ExpenseRepository expenseRepository) {
        this.investmentRepository = investmentRepository;
        this.listingRepository = listingRepository;
        this.harvestRepository = harvestRepository;
        this.apiaryRepository = apiaryRepository;
        this.colonyRepository = colonyRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public InvestorReportResponse investor(AuthenticatedUser actor) {
        List<Investment> orders = investmentRepository.findByInvestorIdOrderByCreatedAtDesc(actor.id());
        List<InvestmentResponse> orderViews = new ArrayList<>();
        BigDecimal pendingAmount = BigDecimal.ZERO;
        BigDecimal activeAmount = BigDecimal.ZERO;
        long pending = 0;
        long active = 0;
        long cancelled = 0;
        List<UUID> apiaryIds = new ArrayList<>();

        for (Investment order : orders) {
            MarketplaceListing listing = listingRepository.findById(order.getListingId()).orElse(null);
            if (listing != null) orderViews.add(InvestmentResponse.from(order, listing));
            if ("PENDING_PAYMENT".equals(order.getStatus())) { pending++; pendingAmount = pendingAmount.add(order.getPrincipalAmount()); }
            else if ("ACTIVE".equals(order.getStatus())) {
                active++;
                activeAmount = activeAmount.add(order.getPrincipalAmount());
                if (listing != null && !apiaryIds.contains(listing.getApiaryId())) apiaryIds.add(listing.getApiaryId());
            } else if ("CANCELLED".equals(order.getStatus())) cancelled++;
        }
        List<HarvestResponse> harvestViews = apiaryIds.isEmpty() ? List.of()
                : harvestRepository.findByApiaryIdInAndStatusInOrderByHarvestedOnDesc(apiaryIds, List.of("APPROVED"))
                    .stream().map(HarvestResponse::from).toList();
        List<ExpenseResponse> expenseViews = apiaryIds.isEmpty() ? List.of()
                : expenseRepository.findByApiaryIdInOrderBySpentOnDesc(apiaryIds).stream().map(ExpenseResponse::from).toList();
        return new InvestorReportResponse(pending, active, cancelled, pendingAmount, activeAmount, orderViews, harvestViews, expenseViews,
                "Ko‘rsatilgan hosil va xarajatlar bog‘langan asalarixonaning operatsion hisobotidir; investor daromadi yoki ulushi sifatida talqin qilinmaydi. ROI va taqsimot tasdiqlanmagan.");
    }

    @Transactional(readOnly = true)
    public BeekeeperReportResponse beekeeper(AuthenticatedUser actor, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.withDayOfYear(1) : from;
        if (end.isAfter(today) || start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hisobot sanalari noto‘g‘ri");
        }
        List<Apiary> apiaries = apiaryRepository.findByOwnerIdOrderByCreatedAtDesc(actor.id());
        List<UUID> ids = apiaries.stream().map(Apiary::getId).toList();
        if (ids.isEmpty()) return new BeekeeperReportResponse(0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, start, end);
        BigDecimal harvestKg = harvestRepository.sumQuantityByApiariesAndPeriod(ids, start, end);
        BigDecimal expenses = expenseRepository.sumAmountByApiariesAndPeriod(ids, start, end);
        return new BeekeeperReportResponse(
                apiaries.size(),
                colonyRepository.countByApiaryIdIn(ids),
                listingRepository.countByBeekeeperIdAndStatus(actor.id(), ListingStatus.ACTIVE),
                listingRepository.countByBeekeeperIdAndStatus(actor.id(), ListingStatus.MODERATION),
                harvestKg == null ? BigDecimal.ZERO : harvestKg,
                expenses == null ? BigDecimal.ZERO : expenses,
                start,
                end
        );
    }
}
