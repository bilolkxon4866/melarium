package uz.melarium.service;

import java.util.List;
import java.util.UUID;

import uz.melarium.api.dto.ListingCreateRequest;
import uz.melarium.api.dto.ListingResponse;
import uz.melarium.api.dto.ListingStatusRequest;
import uz.melarium.api.dto.ListingUpdateRequest;
import uz.melarium.audit.AuditService;
import uz.melarium.domain.Apiary;
import uz.melarium.domain.ListingStatus;
import uz.melarium.domain.MarketplaceListing;
import uz.melarium.repository.MarketplaceListingRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListingService {
    private final MarketplaceListingRepository listingRepository;
    private final ApiaryAccessService apiaryAccessService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public ListingService(MarketplaceListingRepository listingRepository, ApiaryAccessService apiaryAccessService, AuditService auditService, NotificationService notificationService) {
        this.listingRepository = listingRepository;
        this.apiaryAccessService = apiaryAccessService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<ListingResponse> active(String query, int page, int size) {
        return listingRepository.searchByStatus(ListingStatus.ACTIVE, query.trim(), PageRequest.of(page, size)).map(ListingResponse::from);
    }

    @Transactional(readOnly = true)
    public ListingResponse activeById(UUID id) {
        return listingRepository.findByIdAndStatus(id, ListingStatus.ACTIVE).map(ListingResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Taklif topilmadi"));
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> myListings(AuthenticatedUser actor) {
        return listingRepository.findByBeekeeperIdOrderByCreatedAtDesc(actor.id()).stream().map(ListingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<ListingResponse> moderationQueue(int page, int size) {
        return listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.MODERATION, PageRequest.of(page, size)).map(ListingResponse::from);
    }

    @Transactional
    public ListingResponse create(ListingCreateRequest body, AuthenticatedUser actor) {
        Apiary apiary = apiaryAccessService.requireAccess(body.apiaryId(), actor);
        if (!"ACTIVE".equals(apiary.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Faol bo‘lmagan asalarixonada taklif yaratib bo‘lmaydi");
        MarketplaceListing listing = MarketplaceListing.create(actor.id(), apiary.getId(), body.title(), body.description(), "COLONY", body.location(), body.unitPrice(), body.unitsTotal());
        MarketplaceListing saved = listingRepository.save(listing);
        auditService.record(actor.id(), "LISTING_SUBMITTED", "LISTING", saved.getId(), "SUCCESS", java.util.Map.of("status", "MODERATION"));
        return ListingResponse.from(saved);
    }

    @Transactional
    public ListingResponse update(UUID id, ListingUpdateRequest body, AuthenticatedUser actor) {
        MarketplaceListing listing = listingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Taklif topilmadi"));
        boolean admin = actor.roles().contains("ADMIN") || actor.roles().contains("SUPER_ADMIN");
        if (!admin && !listing.getBeekeeperId().equals(actor.id())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Taklif topilmadi");
        if (listing.getStatus() == ListingStatus.SOLD || listing.getStatus() == ListingStatus.RESERVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Band qilingan yoki sotilgan taklifni tahrirlab bo‘lmaydi");
        }
        listing.update(body.title(), body.description(), body.location(), body.unitPrice(), body.unitsTotal());
        MarketplaceListing saved = listingRepository.save(listing);
        auditService.record(actor.id(), "LISTING_UPDATED", "LISTING", id, "SUCCESS", java.util.Map.of("status", "MODERATION"));
        return ListingResponse.from(saved);
    }

    @Transactional
    public ListingResponse moderate(UUID id, ListingStatusRequest body, AuthenticatedUser actor) {
        MarketplaceListing listing = listingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Taklif topilmadi"));
        ListingStatus next = body.status();
        if (next != ListingStatus.ACTIVE && next != ListingStatus.DRAFT && next != ListingStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moderatsiyada ACTIVE, DRAFT yoki INACTIVE holatini tanlana");
        }
        if (listing.getStatus() != ListingStatus.MODERATION && listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Taklifning joriy holati moderatsiyaga ruxsat bermaydi");
        }
        if (next == ListingStatus.ACTIVE && listing.getUnitsAvailable() < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mavjudligi bo‘lmagan taklifni faollashtirib bo‘lmaydi");
        }
        listing.moderate(next, body.note(), actor.id());
        MarketplaceListing saved = listingRepository.save(listing);
        String title = next == ListingStatus.ACTIVE ? "Taklifingiz tasdiqlandi" : "Taklif qayta ko‘rib chiqishga qaytarildi";
        String message = next == ListingStatus.ACTIVE
                ? "“" + saved.getTitle() + "” taklifi investorlar uchun faollashtirildi."
                : "“" + saved.getTitle() + "” taklifiga o‘zgartirish kerak. " + (body.note() == null ? "" : body.note());
        notificationService.create(saved.getBeekeeperId(), title, message, "LISTING");
        auditService.record(actor.id(), "LISTING_MODERATED", "LISTING", id, "SUCCESS", java.util.Map.of("status", next.name(), "note", body.note() == null ? "" : body.note()));
        return ListingResponse.from(saved);
    }
}
