package uz.melarium.service;

import java.util.List;
import java.util.UUID;

import uz.melarium.api.dto.ColonyRequest;
import uz.melarium.audit.AuditService;
import uz.melarium.domain.Apiary;
import uz.melarium.domain.Colony;
import uz.melarium.repository.ApiaryRepository;
import uz.melarium.repository.ColonyRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ColonyService {
    private final ColonyRepository colonyRepository;
    private final ApiaryRepository apiaryRepository;
    private final ApiaryAccessService accessService;
    private final AuditService auditService;

    public ColonyService(ColonyRepository colonyRepository, ApiaryRepository apiaryRepository, ApiaryAccessService accessService, AuditService auditService) {
        this.colonyRepository = colonyRepository;
        this.apiaryRepository = apiaryRepository;
        this.accessService = accessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Colony> list(UUID apiaryId, AuthenticatedUser actor) {
        accessService.requireAccess(apiaryId, actor);
        return colonyRepository.findByApiaryIdOrderByCreatedAtDesc(apiaryId);
    }

    @Transactional
    public Colony create(UUID apiaryId, ColonyRequest body, AuthenticatedUser actor) {
        accessService.requireAccess(apiaryId, actor);
        Colony colony = colonyRepository.save(Colony.create(apiaryId, body.code(), body.count(), body.strength(), body.description()));
        auditService.record(actor.id(), "COLONY_CREATED", "COLONY", colony.getId(), "SUCCESS", java.util.Map.of("apiaryId", apiaryId));
        return colony;
    }

    @Transactional
    public Colony update(UUID colonyId, ColonyRequest body, AuthenticatedUser actor) {
        Colony colony = colonyRepository.findById(colonyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ari oilasi topilmadi"));
        Apiary apiary = accessService.requireAccess(colony.getApiaryId(), actor);
        colony.update(body.strength(), "ACTIVE", body.description());
        Colony saved = colonyRepository.save(colony);
        auditService.record(actor.id(), "COLONY_UPDATED", "COLONY", colonyId, "SUCCESS", java.util.Map.of("apiaryId", apiary.getId()));
        return saved;
    }

    @Transactional
    public void deactivate(UUID colonyId, AuthenticatedUser actor) {
        Colony colony = colonyRepository.findById(colonyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ari oilasi topilmadi"));
        accessService.requireAccess(colony.getApiaryId(), actor);
        colony.update(null, "INACTIVE", null);
        colonyRepository.save(colony);
        auditService.record(actor.id(), "COLONY_DEACTIVATED", "COLONY", colonyId, "SUCCESS", java.util.Map.of());
    }
}
