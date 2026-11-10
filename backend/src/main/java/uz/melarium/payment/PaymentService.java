package uz.melarium.payment;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import uz.melarium.api.dto.PaymentInitiateRequest;
import uz.melarium.api.dto.PaymentInitiationResponse;
import uz.melarium.api.dto.PaymentResponse;
import uz.melarium.api.dto.PaymentWebhookPayload;
import uz.melarium.api.dto.PaymentWebhookResponse;
import uz.melarium.audit.AuditService;
import uz.melarium.domain.Investment;
import uz.melarium.domain.MarketplaceListing;
import uz.melarium.domain.Payment;
import uz.melarium.domain.UserAccount;
import uz.melarium.repository.InvestmentRepository;
import uz.melarium.repository.MarketplaceListingRepository;
import uz.melarium.repository.PaymentRepository;
import uz.melarium.repository.PaymentWebhookEventRepository;
import uz.melarium.repository.UserAccountRepository;
import uz.melarium.security.AuthenticatedUser;
import uz.melarium.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {
    private static final Set<String> PROVIDERS = Set.of("CLICK", "PAYME", "UZCARD", "HUMO");
    private final PaymentRepository paymentRepository;
    private final InvestmentRepository investmentRepository;
    private final MarketplaceListingRepository listingRepository;
    private final UserAccountRepository userRepository;
    private final PaymentWebhookEventRepository eventRepository;
    private final List<PaymentGateway> gateways;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final Map<String, String> webhookSecrets;
    private final boolean paymentsEnabled;
    private final String approvedTermsVersion;

    public PaymentService(PaymentRepository paymentRepository,
                          InvestmentRepository investmentRepository,
                          MarketplaceListingRepository listingRepository,
                          UserAccountRepository userRepository,
                          PaymentWebhookEventRepository eventRepository,
                          List<PaymentGateway> gateways,
                          ObjectMapper objectMapper,
                          Validator validator,
                          NotificationService notificationService,
                          AuditService auditService,
                          @Value("${app.payments.webhook-secrets.click:}") String clickSecret,
                          @Value("${app.payments.webhook-secrets.payme:}") String paymeSecret,
                          @Value("${app.payments.webhook-secrets.uzcard:}") String uzcardSecret,
                          @Value("${app.payments.webhook-secrets.humo:}") String humoSecret,
                          @Value("${app.payments.enabled:false}") boolean paymentsEnabled,
                          @Value("${app.investment.terms-version:}") String approvedTermsVersion) {
        this.paymentRepository = paymentRepository;
        this.investmentRepository = investmentRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.gateways = gateways;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.webhookSecrets = Map.of("CLICK", nullToEmpty(clickSecret), "PAYME", nullToEmpty(paymeSecret),
                "UZCARD", nullToEmpty(uzcardSecret), "HUMO", nullToEmpty(humoSecret));
        this.paymentsEnabled = paymentsEnabled;
        this.approvedTermsVersion = nullToEmpty(approvedTermsVersion).trim();
    }

    @Transactional
    public PaymentInitiationResponse initiate(PaymentInitiateRequest body, AuthenticatedUser actor) {
        String provider = body.provider().toUpperCase();
        Investment order = investmentRepository.findByIdAndInvestorId(body.investmentId(), actor.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyurtma topilmadi"));
        if (!"PENDING_PAYMENT".equals(order.getStatus()) || order.reservationExpired(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buyurtma to‘lov uchun faol emas; mavjudlikni qayta tekshiring");
        }
        if (approvedTermsVersion.isBlank() || order.getTermsAcceptedAt() == null || !approvedTermsVersion.equals(order.getTermsVersion())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Tasdiqlangan investitsiya shartlari mavjud emas; to‘lovlar o‘chiq");
        }
        if (!paymentsEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Haqiqiy to‘lov provayderi sozlanmagan; hech qanday mablag‘ olinmadi");
        }
        PaymentGateway gateway = gateways.stream()
                .filter(candidate -> provider.equals(candidate.provider()) && candidate.isConfigured())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Tanlangan to‘lov provayderi ulanmagan"));

        var existing = paymentRepository.findByInvestmentIdAndIdempotencyKey(order.getId(), body.idempotencyKey());
        if (existing.isPresent()) {
            Payment payment = existing.get();
            return new PaymentInitiationResponse(payment.getId(), order.getId(), payment.getProvider(), payment.getAmount(),
                    payment.getCurrency(), payment.getStatus(), null, order.getReservationExpiresAt());
        }
        Payment payment = Payment.create(order.getId(), provider, body.idempotencyKey(), order.getPrincipalAmount());
        PaymentGateway.CheckoutSession session = gateway.initiate(payment, order);
        payment.applyProviderResult("PENDING", session.providerReference(), null);
        Payment saved = paymentRepository.save(payment);
        auditService.record(actor.id(), "PAYMENT_INITIATED", "PAYMENT", saved.getId(), "SUCCESS",
                Map.of("provider", provider, "amount", saved.getAmount(), "investmentId", order.getId()));
        return new PaymentInitiationResponse(saved.getId(), order.getId(), provider, saved.getAmount(), saved.getCurrency(),
                saved.getStatus(), session.redirectUrl(), order.getReservationExpiresAt());
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> myPayments(int page, int size, AuthenticatedUser actor) {
        return paymentRepository.findByInvestorId(actor.id(), PageRequest.of(page, size)).map(PaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> adminPayments(int page, int size) {
        return paymentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).map(PaymentResponse::from);
    }

    @Transactional
    public PaymentWebhookResponse acceptWebhook(String rawProvider, String signature, String rawBody) {
        String provider = rawProvider == null ? "" : rawProvider.trim().toUpperCase();
        if (!PROVIDERS.contains(provider)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "To‘lov provayderi qo‘llab-quvvatlanmaydi");
        String secret = webhookSecrets.get(provider);
        if (secret == null || secret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Provayder webhook siri sozlanmagan");
        }
        if (rawBody == null || rawBody.isBlank() || rawBody.length() > 64_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook so‘rovi bo‘sh yoki juda katta");
        }
        if (!validSignature(secret, rawBody, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook imzosi noto‘g‘ri");
        }

        PaymentWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, PaymentWebhookPayload.class);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook JSON formati noto‘g‘ri");
        }
        Set<ConstraintViolation<PaymentWebhookPayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook maydonlarini tekshiring");
        String payloadHash = sha256(rawBody);
        var previousHash = eventRepository.findPayloadHash(provider, payload.eventId());
        if (previousHash.isPresent()) {
            if (!MessageDigest.isEqual(previousHash.get().getBytes(StandardCharsets.US_ASCII), payloadHash.getBytes(StandardCharsets.US_ASCII))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Webhook event ID boshqa payload bilan avval ishlatilgan");
            }
            return new PaymentWebhookResponse(payload.paymentId(), payload.status(), true, "PAYMENT_REVIEW".equals(payload.status()));
        }

        Payment payment = paymentRepository.findByIdAndProviderForUpdate(payload.paymentId(), provider)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "To‘lov topilmadi"));
        if (payment.getAmount().compareTo(payload.amount()) != 0 || !payment.getCurrency().equals(payload.currency())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Webhook summasi yoki valyutasi to‘lovga mos emas");
        }
        var byTransaction = paymentRepository.findByProviderAndProviderTransactionId(provider, payload.providerTransactionId());
        if (byTransaction.isPresent() && !byTransaction.get().getId().equals(payment.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Provayder tranzaksiya identifikatori takrorlangan");
        }
        if ("REFUNDED".equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Qaytarilgan to‘lov holatini o‘zgartirib bo‘lmaydi");
        }
        if ("SUCCESS".equals(payment.getStatus()) && !"REFUNDED".equals(payload.status())) {
            if (payment.getProviderTransactionId() != null && payment.getProviderTransactionId().equals(payload.providerTransactionId())) {
                return new PaymentWebhookResponse(payment.getId(), payment.getStatus(), true, false);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "To‘lov allaqachon yakuniy holatga o‘tgan");
        }
        if ("REFUNDED".equals(payload.status()) && !"SUCCESS".equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Faqat muvaffaqiyatli to‘lovni refund qilish mumkin");
        }

        boolean manualReviewRequired = false;
        Investment order = investmentRepository.findByIdForUpdate(payment.getInvestmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "To‘lov bilan bog‘liq buyurtma topilmadi"));
        if ("SUCCESS".equals(payload.status())) {
            manualReviewRequired = !canActivate(order);
            if (manualReviewRequired) {
                if ("PENDING_PAYMENT".equals(order.getStatus())) releaseReservation(order);
                if ("PENDING_PAYMENT".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) order.markPaymentReview();
            } else {
                MarketplaceListing listing = listingRepository.findByIdForUpdate(order.getListingId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Taklif topilmadi"));
                order.activate();
                listing.confirmReservedUnits();
                listingRepository.save(listing);
            }
        } else if ("REFUNDED".equals(payload.status())) {
            if ("PENDING_PAYMENT".equals(order.getStatus())) releaseReservation(order);
            order.markRefunded();
        }

        payment.applyProviderResult(payload.status(), payload.providerTransactionId(), Instant.now());
        paymentRepository.save(payment);
        investmentRepository.save(order);
        eventRepository.insert(provider, payload.eventId(), payloadHash, payment.getId(), "SUCCESS");
        String outcome = manualReviewRequired ? "PAYMENT_REVIEW" : payload.status();
        auditService.record(null, "PAYMENT_WEBHOOK_" + payload.status(), "PAYMENT", payment.getId(), "SUCCESS",
                Map.of("provider", provider, "transactionId", payload.providerTransactionId(), "outcome", outcome));
        UUID recipient = order.getInvestorId();
        String message = manualReviewRequired
                ? "Provayder to‘lovni tasdiqladi, ammo buyurtma avtomatik faollashtirilmadi. Operator tekshiruvi talab qilinadi."
                : "“" + listingTitle(order.getListingId()) + "” buyurtmangiz bo‘yicha to‘lov holati: " + payload.status() + ".";
        notificationService.create(recipient, manualReviewRequired ? "To‘lov qo‘lda tekshiriladi" : "To‘lov holati yangilandi", message, "PAYMENT");
        return new PaymentWebhookResponse(payment.getId(), payment.getStatus(), false, manualReviewRequired);
    }

    private boolean canActivate(Investment order) {
        UserAccount investor = userRepository.findById(order.getInvestorId()).orElse(null);
        return "PENDING_PAYMENT".equals(order.getStatus())
                && !order.reservationExpired(Instant.now())
                && order.getTermsAcceptedAt() != null
                && !approvedTermsVersion.isBlank()
                && approvedTermsVersion.equals(order.getTermsVersion())
                && investor != null
                && "ACTIVE".equals(investor.getAccountStatus());
    }

    private void releaseReservation(Investment order) {
        MarketplaceListing listing = listingRepository.findByIdForUpdate(order.getListingId()).orElse(null);
        if (listing == null) return;
        listing.releaseUnits(order.getUnitsQuantity());
        listingRepository.save(listing);
    }

    private String listingTitle(UUID listingId) {
        return listingRepository.findById(listingId).map(MarketplaceListing::getTitle).orElse("MELARIUM");
    }

    private boolean validSignature(String secret, String body, String header) {
        if (header == null || header.isBlank()) return false;
        String supplied = header.trim().replaceFirst("(?i)^sha256=", "").toLowerCase();
        String expected = hmacSha256(secret, body);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), supplied.getBytes(StandardCharsets.US_ASCII));
    }

    private String hmacSha256(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Webhook HMAC hisoblanmadi", exception);
        }
    }

    private String sha256(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Webhook hash hisoblanmadi", exception);
        }
    }

    private static String nullToEmpty(String value) { return value == null ? "" : value; }
}
