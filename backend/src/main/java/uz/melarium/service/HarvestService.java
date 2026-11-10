package uz.melarium.service;

import java.util.List;
import java.util.UUID;

import uz.melarium.api.dto.HarvestRequest;
import uz.melarium.audit.AuditService;
import uz.melarium.domain.Colony;
import uz.melarium.domain.Harvest;
import uz.melarium.repository.ColonyRepository;
import uz.melarium.repository.HarvestRepository;
import uz.melarium.repository.InvestmentRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HarvestService {
    private final HarvestRepository harvestRepository;
    private final ColonyRepository colonyRepository;
    private final ApiaryAccessService apiaryAccessService;
    private final AuditService auditService;
    private final InvestmentRepository investmentRepository;
    private final NotificationService notificationService;

    public HarvestService(HarvestRepository harvestRepository, ColonyRepository colonyRepository, ApiaryAccessService apiaryAccessService,
                          AuditService auditService, InvestmentRepository investmentRepository, NotificationService notificationService) {
        this.harvestRepository = harvestRepository;
        this.colonyRepository = colonyRepository;
        this.apiaryAccessService = apiaryAccessService;
        this.auditService = auditService;
        this.investmentRepository = investmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<Harvest> list(AuthenticatedUser actor) {
        if (isAdmin(actor)) return harvestRepository.findAllByOrderByHarvestedOnDesc();
        return harvestRepository.findByReportedByOrderByHarvestedOnDesc(actor.id());
    }

    @Transactional
    public Harvest create(HarvestRequest body, AuthenticatedUser actor) {
        apiaryAccessService.requireAccess(body.apiaryId(), actor);
        if (body.colonyId() != null) verifyColony(body.colonyId(), body.apiaryId());
        Harvest saved = harvestRepository.save(Harvest.create(body.apiaryId(), body.colonyId(), actor.id(), body.quantityKg(), body.harvestedOn(), body.note()));
        auditService.record(actor.id(), "HARVEST_SUBMITTED", "HARVEST", saved.getId(), "SUCCESS", java.util.Map.of("apiaryId", body.apiaryId()));
        for (UUID investorId : investmentRepository.findActiveInvestorIdsForApiary(body.apiaryId())) {
            notificationService.create(investorId, "Yangi hosil hisoboti", "Bog‘langan asalarixonada yangi hosil hisoboti yuborildi; administrator tekshiruvidan so‘ng ko‘rinadi.", "REPORT");
        }
        return saved;
    }

    @Transactional
    public Harvest review(UUID id, String status, AuthenticatedUser actor) {
        Harvest harvest = harvestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hosil hisoboti topilmadi"));
        harvest.review(status, actor.id());
        Harvest saved = harvestRepository.save(harvest);
        auditService.record(actor.id(), "HARVEST_REVIEWED", "HARVEST", id, "SUCCESS", java.util.Map.of("status", status));
        notificationService.create(harvest.getReportedBy(), "Hosil hisoboti ko‘rib chiqildi",
                status.equals("APPROVED") ? "Hosil hisobotingiz tasdiqlandi." : "Hosil hisobotingizga tuzatish kiritish so‘raldi.", "REPORT");
        return saved;
    }

    private void verifyColony(UUID colonyId, UUID apiaryId) {
        Colony colony = colonyRepository.findByIdAndApiaryId(colonyId, apiaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ari oilasi ko‘rsatilgan asalarixonaga tegishli emas"));
        if ("INACTIVE".equals(colony.getStatus()) || "LOST".equals(colony.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Faol bo‘lmagan ari oilasi uchun hosil qaydi yaratib bo‘lmaydi");
        }
    }

    private boolean isAdmin(AuthenticatedUser actor) { return actor.roles().contains("ADMIN") || actor.roles().contains("SUPER_ADMIN"); }
}
