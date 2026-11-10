package uz.melarium.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import uz.melarium.api.dto.CreateInvestmentRequest;
import uz.melarium.api.dto.InvestmentResponse;
import uz.melarium.audit.AuditService;
import uz.melarium.domain.Investment;
import uz.melarium.domain.ListingStatus;
import uz.melarium.domain.MarketplaceListing;
import uz.melarium.repository.InvestmentRepository;
import uz.melarium.repository.MarketplaceListingRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InvestmentService {
    private final InvestmentRepository investmentRepository;
    private final MarketplaceListingRepository listingRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final Duration reservationTtl;
    private final String approvedTermsVersion;

    public InvestmentService(InvestmentRepository investmentRepository,
                             MarketplaceListingRepository listingRepository,
                             NotificationService notificationService,
                             AuditService auditService,
                             @Value("${app.investment.reservation-ttl:PT15M}") Duration reservationTtl,
                             @Value("${app.investment.terms-version:}") String approvedTermsVersion) {
        this.investmentRepository = investmentRepository;
        this.listingRepository = listingRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.reservationTtl = reservationTtl;
        this.approvedTermsVersion = approvedTermsVersion == null ? "" : approvedTermsVersion.trim();
    }

    @Transactional
    public InvestmentResponse createOrder(CreateInvestmentRequest body, AuthenticatedUser actor) {
        String idempotencyKey = body.idempotencyKey() == null || body.idempotencyKey().isBlank() ? null : body.idempotencyKey().trim();
        if (idempotencyKey != null) {
            var existing = investmentRepository.findByInvestorIdAndIdempotencyKey(actor.id(), idempotencyKey);
            if (existing.isPresent()) {
                Investment order = existing.get();
                if (!order.getListingId().equals(body.listingId()) || !order.getUnitsQuantity().equals(body.unitsQuantity())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key boshqa buyurtmada ishlatilgan");
                }
                MarketplaceListing existingListing = listingRepository.findById(order.getListingId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Taklif topilmadi"));
                return InvestmentResponse.from(order, existingListing);
            }
        }

        MarketplaceListing listing = listingRepository.findByIdForUpdate(body.listingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Taklif topilmadi"));
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Taklif faol emas yoki band qilingan");
        }
        if (body.unitsQuantity() > listing.getUnitsAvailable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "So‘ralgan miqdor qolgan mavjudlikdan ko‘p");
        }

        String acceptedTermsVersion = null;
        Instant termsAcceptedAt = null;
        if (!approvedTermsVersion.isBlank()) {
            if (!Boolean.TRUE.equals(body.acceptTerms()) || !approvedTermsVersion.equals(body.termsVersion())) {
                throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Amaldagi investitsiya shartlarini ko‘rib chiqib tasdiqlash talab qilinadi");
            }
            acceptedTermsVersion = approvedTermsVersion;
            termsAcceptedAt = Instant.now();
        }
        BigDecimal amount = listing.getUnitPrice().multiply(BigDecimal.valueOf(body.unitsQuantity())).setScale(2, RoundingMode.HALF_UP);
        Instant expiresAt = Instant.now().plus(reservationTtl);
        listing.reserveUnits(body.unitsQuantity());
        listingRepository.save(listing);
        Investment saved = investmentRepository.save(Investment.create(actor.id(), listing.getId(), body.unitsQuantity(), amount,
                expiresAt, idempotencyKey, acceptedTermsVersion, termsAcceptedAt));
        notificationService.create(actor.id(), "Buyurtmangiz vaqtincha band qilindi",
                "“" + listing.getTitle() + "” taklifi bo‘yicha " + body.unitsQuantity() + " birlik " + reservationTtl.toMinutes() + " daqiqagacha saqlandi. Hozircha to‘lov olinmadi.", "INVESTMENT");
        notificationService.create(listing.getBeekeeperId(), "Yangi buyurtma so‘rovi",
                "“" + listing.getTitle() + "” taklifi bo‘yicha vaqtinchalik buyurtma yaratildi.", "INVESTMENT");
        auditService.record(actor.id(), "INVESTMENT_ORDER_CREATED", "INVESTMENT", saved.getId(), "SUCCESS",
                Map.of("listingId", listing.getId(), "units", body.unitsQuantity(), "amount", amount, "status", "PENDING_PAYMENT"));
        return InvestmentResponse.from(saved, listing);
    }

    @Transactional(readOnly = true)
    public List<InvestmentResponse> myOrders(AuthenticatedUser actor) {
        return investmentRepository.findByInvestorIdOrderByCreatedAtDesc(actor.id()).stream()
                .map(order -> InvestmentResponse.from(order, requireListing(order.getListingId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<InvestmentResponse> adminOrders(int page, int size) {
        Page<Investment> orders = investmentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return orders.map(order -> InvestmentResponse.from(order, requireListing(order.getListingId())));
    }

    @Transactional(readOnly = true)
    public InvestmentResponse get(UUID id, AuthenticatedUser actor) {
        Investment order = investmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyurtma topilmadi"));
        boolean admin = actor.roles().contains("ADMIN") || actor.roles().contains("SUPER_ADMIN");
        if (!admin && !order.getInvestorId().equals(actor.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyurtma topilmadi");
        }
        return InvestmentResponse.from(order, requireListing(order.getListingId()));
    }

    @Transactional
    public InvestmentResponse cancel(UUID id, AuthenticatedUser actor) {
        Investment order = investmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyurtma topilmadi"));
        boolean admin = actor.roles().contains("ADMIN") || actor.roles().contains("SUPER_ADMIN");
        if (!admin && !order.getInvestorId().equals(actor.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyurtma topilmadi");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Faqat to‘lov kutilayotgan buyurtmani bekor qilish mumkin");
        }
        releaseReservation(order);
        order.cancel();
        investmentRepository.save(order);
        auditService.record(actor.id(), "INVESTMENT_ORDER_CANCELLED", "INVESTMENT", id, "SUCCESS", Map.of());
        notificationService.create(actor.id(), "Buyurtma bekor qilindi", "Vaqtinchalik buyurtma bekor qilindi; to‘lov olinmagan.", "INVESTMENT");
        return InvestmentResponse.from(order, requireListing(order.getListingId()));
    }

    @Transactional
    public int expireReservations() {
        List<Investment> candidates = investmentRepository.findByStatusAndReservationExpiresAtBeforeOrderByReservationExpiresAtAsc("PENDING_PAYMENT", Instant.now());
        int expired = 0;
        for (Investment candidate : candidates) {
            Investment order = investmentRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (order == null || !order.reservationExpired(Instant.now())) continue;
            releaseReservation(order);
            order.expire();
            investmentRepository.save(order);
            notificationService.create(order.getInvestorId(), "Buyurtma muddati tugadi", "Vaqtinchalik band qilish muddati tugadi; to‘lov amalga oshmadi.", "INVESTMENT");
            auditService.record(null, "INVESTMENT_ORDER_EXPIRED", "INVESTMENT", order.getId(), "SUCCESS", Map.of());
            expired++;
        }
        return expired;
    }

    private void releaseReservation(Investment order) {
        MarketplaceListing listing = listingRepository.findByIdForUpdate(order.getListingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Taklif topilmadi; band qilingan miqdorni tekshirish kerak"));
        listing.releaseUnits(order.getUnitsQuantity());
        listingRepository.save(listing);
    }

    private MarketplaceListing requireListing(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Taklif topilmadi"));
    }
}
