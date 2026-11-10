package uz.melarium.api;

import jakarta.validation.Valid;
import uz.melarium.api.dto.UpdateProfileRequest;
import uz.melarium.api.dto.UserProfileResponse;
import uz.melarium.audit.AuditService;
import uz.melarium.auth.UserRoleService;
import uz.melarium.domain.UserAccount;
import uz.melarium.repository.UserAccountRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserAccountRepository userRepository;
    private final UserRoleService roleService;
    private final AuditService auditService;

    public UserController(UserAccountRepository userRepository, UserRoleService roleService, AuditService auditService) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.auditService = auditService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        UserAccount user = getUser(principal);
        return UserProfileResponse.from(user, roleService.rolesFor(user.getId()));
    }

    @PatchMapping("/me")
    @Transactional
    public UserProfileResponse updateMe(@AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody UpdateProfileRequest body) {
        UserAccount user = getUser(principal);
        user.updateProfile(body.fullName(), body.email());
        user = userRepository.save(user);
        auditService.record(user.getId(), "USER_PROFILE_UPDATED", "USER", user.getId(), "SUCCESS", java.util.Map.of());
        return UserProfileResponse.from(user, roleService.rolesFor(user.getId()));
    }

    private UserAccount getUser(AuthenticatedUser principal) {
        return userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foydalanuvchi topilmadi"));
    }
}
