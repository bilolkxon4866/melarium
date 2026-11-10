package uz.melarium.service;

import uz.melarium.api.dto.AdminDashboardResponse;
import uz.melarium.auth.UserRoleService;
import uz.melarium.domain.ListingStatus;
import uz.melarium.repository.HarvestRepository;
import uz.melarium.repository.InvestmentRepository;
import uz.melarium.repository.MarketplaceListingRepository;
import uz.melarium.repository.PaymentRepository;
import uz.melarium.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private final UserAccountRepository userRepository;
    private final UserRoleService roleService;
    private final MarketplaceListingRepository listingRepository;
    private final InvestmentRepository investmentRepository;
    private final PaymentRepository paymentRepository;
    private final HarvestRepository harvestRepository;

    public AdminDashboardService(UserAccountRepository userRepository, UserRoleService roleService,
                                 MarketplaceListingRepository listingRepository, InvestmentRepository investmentRepository,
                                 PaymentRepository paymentRepository, HarvestRepository harvestRepository) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.listingRepository = listingRepository;
        this.investmentRepository = investmentRepository;
        this.paymentRepository = paymentRepository;
        this.harvestRepository = harvestRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse get() {
        return new AdminDashboardResponse(
                userRepository.count(),
                userRepository.countByAccountStatus("ACTIVE"),
                roleService.countUsersWithRole("INVESTOR"),
                roleService.countUsersWithRole("BEEKEEPER"),
                userRepository.countByAccountStatus("SUSPENDED"),
                listingRepository.countByStatus(ListingStatus.ACTIVE),
                listingRepository.countByStatus(ListingStatus.MODERATION),
                investmentRepository.countByStatus("PENDING_PAYMENT"),
                investmentRepository.countByStatus("PAYMENT_REVIEW"),
                investmentRepository.countByStatus("ACTIVE"),
                paymentRepository.countByStatus("PENDING"),
                paymentRepository.countByStatus("SUCCESS"),
                harvestRepository.countByStatus("SUBMITTED")
        );
    }
}
