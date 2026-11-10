package uz.melarium.service;

import java.util.List;

import uz.melarium.api.dto.ExpenseRequest;
import uz.melarium.audit.AuditService;
import uz.melarium.domain.Colony;
import uz.melarium.domain.Expense;
import uz.melarium.repository.ColonyRepository;
import uz.melarium.repository.ExpenseRepository;
import uz.melarium.repository.InvestmentRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final ColonyRepository colonyRepository;
    private final ApiaryAccessService apiaryAccessService;
    private final AuditService auditService;
    private final InvestmentRepository investmentRepository;
    private final NotificationService notificationService;

    public ExpenseService(ExpenseRepository expenseRepository, ColonyRepository colonyRepository, ApiaryAccessService apiaryAccessService,
                          AuditService auditService, InvestmentRepository investmentRepository, NotificationService notificationService) {
        this.expenseRepository = expenseRepository;
        this.colonyRepository = colonyRepository;
        this.apiaryAccessService = apiaryAccessService;
        this.auditService = auditService;
        this.investmentRepository = investmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<Expense> list(AuthenticatedUser actor) {
        if (actor.roles().contains("ADMIN") || actor.roles().contains("SUPER_ADMIN")) return expenseRepository.findAllByOrderBySpentOnDesc();
        return expenseRepository.findByReportedByOrderBySpentOnDesc(actor.id());
    }

    @Transactional
    public Expense create(ExpenseRequest body, AuthenticatedUser actor) {
        apiaryAccessService.requireAccess(body.apiaryId(), actor);
        if (body.colonyId() != null) {
            Colony colony = colonyRepository.findByIdAndApiaryId(body.colonyId(), body.apiaryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ari oilasi ko‘rsatilgan asalarixonaga tegishli emas"));
            if ("INACTIVE".equals(colony.getStatus()) || "LOST".equals(colony.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Faol bo‘lmagan ari oilasi uchun xarajat qaydi yaratib bo‘lmaydi");
            }
        }
        Expense saved = expenseRepository.save(Expense.create(body.apiaryId(), body.colonyId(), actor.id(), body.category(), body.amount(), body.spentOn(), body.note()));
        auditService.record(actor.id(), "EXPENSE_CREATED", "EXPENSE", saved.getId(), "SUCCESS", java.util.Map.of("apiaryId", body.apiaryId(), "amount", body.amount()));
        for (java.util.UUID investorId : investmentRepository.findActiveInvestorIdsForApiary(body.apiaryId())) {
            notificationService.create(investorId, "Xarajat qaydi yangilandi", "Bog‘langan asalarixonada yangi xarajat qaydi kiritildi.", "REPORT");
        }
        return saved;
    }
}
