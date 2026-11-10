package uz.melarium.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import uz.melarium.api.dto.AdminUserResponse;
import uz.melarium.api.dto.PageResponse;
import uz.melarium.api.dto.RoleUpdateRequest;
import uz.melarium.api.dto.UserStatusRequest;
import uz.melarium.audit.AuditService;
import uz.melarium.auth.TokenService;
import uz.melarium.auth.UserRoleService;
import uz.melarium.domain.UserAccount;
import uz.melarium.repository.UserAccountRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final UserAccountRepository userRepository;
    private final UserRoleService roleService;
    private final TokenService tokenService;
    private final AuditService auditService;

    public AdminUserController(UserAccountRepository userRepository, UserRoleService roleService,
                               TokenService tokenService, AuditService auditService) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.tokenService = tokenService;
        this.auditService = auditService;
    }

    @GetMapping
    public PageResponse<AdminUserResponse> list(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        if (status != null && !List.of("ACTIVE", "PENDING", "SUSPENDED", "CLOSED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hisob holati filtri noto‘g‘ri");
        }
        Page<UserAccount> result = userRepository.searchUsers(query.trim(), status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result.map(user -> AdminUserResponse.from(user, roleService.rolesFor(user.getId()))));
    }

    @PatchMapping("/{userId}/roles")
    public AdminUserResponse updateRoles(
            @PathVariable UUID userId,
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody RoleUpdateRequest body
    ) {
        UserAccount target = findUser(userId);
        List<String> currentRoles = roleService.rolesFor(userId);
        boolean assignsElevatedRole = body.roles().stream().anyMatch(role -> role.equals("ADMIN") || role.equals("SUPER_ADMIN"));
        if (assignsElevatedRole && !actor.roles().contains("SUPER_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator rolini faqat Super Admin tayinlaydi");
        }
        if (currentRoles.contains("SUPER_ADMIN") && !actor.roles().contains("SUPER_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super Admin rolini faqat Super Admin boshqaradi");
        }
        if (currentRoles.contains("SUPER_ADMIN") && !body.roles().contains("SUPER_ADMIN") && roleService.countUsersWithRole("SUPER_ADMIN") <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Oxirgi Super Admin rolini olib tashlab bo‘lmaydi");
        }
        if (actor.id().equals(userId) && !body.roles().contains("ADMIN") && !body.roles().contains("SUPER_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O‘zingizning administrator rolingizni olib tashlay olmaysiz");
        }
        roleService.replaceRoles(userId, body.roles(), actor.id());
        List<String> updatedRoles = roleService.rolesFor(userId);
        auditService.record(actor.id(), "USER_ROLES_UPDATED", "USER", userId, "SUCCESS", java.util.Map.of("roles", updatedRoles));
        return AdminUserResponse.from(target, updatedRoles);
    }

    @PatchMapping("/{userId}/status")
    public AdminUserResponse updateStatus(
            @PathVariable UUID userId,
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody UserStatusRequest body
    ) {
        UserAccount target = findUser(userId);
        List<String> roles = roleService.rolesFor(userId);
        if (roles.contains("SUPER_ADMIN") && !actor.roles().contains("SUPER_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super Admin hisobini faqat Super Admin boshqaradi");
        }
        if (actor.id().equals(userId) && !"ACTIVE".equals(body.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Joriy administrator o‘z hisobini to‘xtata olmaydi");
        }
        target.changeAccountStatus(body.status());
        UserAccount saved = userRepository.save(target);
        if (!"ACTIVE".equals(body.status())) tokenService.revokeAllForUser(userId);
        auditService.record(actor.id(), "USER_STATUS_UPDATED", "USER", userId, "SUCCESS", java.util.Map.of("status", body.status()));
        return AdminUserResponse.from(saved, roles);
    }

    private UserAccount findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foydalanuvchi topilmadi"));
    }
}
