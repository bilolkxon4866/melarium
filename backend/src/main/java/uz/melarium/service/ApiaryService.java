package uz.melarium.service;

import java.util.List;
import java.util.UUID;

import uz.melarium.api.dto.ApiaryRequest;
import uz.melarium.audit.AuditService;
import uz.melarium.domain.Apiary;
import uz.melarium.repository.ApiaryRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiaryService {
    private final ApiaryRepository apiaryRepository;
    private final ApiaryAccessService accessService;
    private final AuditService auditService;

    public ApiaryService(ApiaryRepository apiaryRepository, ApiaryAccessService accessService, AuditService auditService) {
        this.apiaryRepository = apiaryRepository;
        this.accessService = accessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Apiary> list(AuthenticatedUser actor) {
        if (isAdmin(actor)) return apiaryRepository.findAll();
        return apiaryRepository.findByOwnerIdOrderByCreatedAtDesc(actor.id());
    }

    @Transactional(readOnly = true)
    public Apiary get(UUID id, AuthenticatedUser actor) { return accessService.requireAccess(id, actor); }

    @Transactional
    public Apiary create(ApiaryRequest body, AuthenticatedUser actor) {
        Apiary saved = apiaryRepository.save(Apiary.create(actor.id(), body.name(), body.location(), body.description()));
        auditService.record(actor.id(), "APIARY_CREATED", "APIARY", saved.getId(), "SUCCESS", java.util.Map.of());
        return saved;
    }

    @Transactional
    public Apiary update(UUID id, ApiaryRequest body, AuthenticatedUser actor) {
        Apiary apiary = accessService.requireAccess(id, actor);
        apiary.update(body.name(), body.location(), body.description(), null);
        Apiary saved = apiaryRepository.save(apiary);
        auditService.record(actor.id(), "APIARY_UPDATED", "APIARY", saved.getId(), "SUCCESS", java.util.Map.of());
        return saved;
    }

    @Transactional
    public void deactivate(UUID id, AuthenticatedUser actor) {
        Apiary apiary = accessService.requireAccess(id, actor);
        apiary.update(null, null, null, "INACTIVE");
        apiaryRepository.save(apiary);
        auditService.record(actor.id(), "APIARY_DEACTIVATED", "APIARY", id, "SUCCESS", java.util.Map.of());
    }

    private boolean isAdmin(AuthenticatedUser actor) {
        return actor.roles().contains("ADMIN") || actor.roles().contains("SUPER_ADMIN");
    }
}
