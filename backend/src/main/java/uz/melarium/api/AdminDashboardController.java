package uz.melarium.api;

import uz.melarium.api.dto.AdminDashboardResponse;
import uz.melarium.service.AdminDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/admin")
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) { this.dashboardService = dashboardService; }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() { return dashboardService.get(); }
}
