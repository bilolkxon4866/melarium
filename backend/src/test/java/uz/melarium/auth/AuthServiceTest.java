package uz.melarium.auth;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import uz.melarium.audit.AuditService;
import uz.melarium.domain.OtpChallenge;
import uz.melarium.domain.UserAccount;
import uz.melarium.repository.OtpChallengeRepository;
import uz.melarium.repository.UserAccountRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock OtpChallengeRepository challengeRepository;
    @Mock UserAccountRepository userRepository;
    @Mock OtpHasher otpHasher;
    @Mock SmsSender smsSender;
    @Mock UserRoleService userRoleService;
    @Mock TokenService tokenService;
    @Mock AuditService auditService;
    @Mock Environment environment;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(challengeRepository, userRepository, otpHasher, smsSender,
                userRoleService, tokenService, auditService, environment, false,
                "Admin@Example.com", "+998901234567",
                "SuperAdmin@Example.com", "+998907654321");
    }

    @Test
    void otpChallengeStoresNormalizedEmailAndPhone() {
        when(otpHasher.hash(anyString())).thenReturn("test-hash");
        when(challengeRepository.findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc("+998901234567"))
                .thenReturn(Optional.empty());

        service.requestOtp("998901234567", "  User@Example.COM  ", "192.0.2.1");

        ArgumentCaptor<OtpChallenge> challenge = ArgumentCaptor.forClass(OtpChallenge.class);
        verify(challengeRepository).save(challenge.capture());
        assertEquals("+998901234567", challenge.getValue().getPhone());
        assertEquals("test-hash", challenge.getValue().getEmailHash());
        verify(otpHasher).hash("email:user@example.com");
        verify(smsSender).sendOtp(eq("+998901234567"), anyString());
    }

    @Test
    void verificationMustUseEmailBoundToTheOtpChallenge() {
        when(otpHasher.hash("email:different@example.com")).thenReturn("different-hash");
        OtpChallenge challenge = OtpChallenge.create("+998901234567", "user-hash", "test-hash",
                Instant.now().plusSeconds(300), null);
        when(challengeRepository.findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc("+998901234567"))
                .thenReturn(Optional.of(challenge));

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.verifyOtp("+998901234567", "different@example.com", "123456"));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void productionRequiresBothPrivilegedRolePairs() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> new AuthService(challengeRepository, userRepository,
                otpHasher, smsSender, userRoleService, tokenService, auditService, environment, false,
                "admin@example.com", "+998901234567", "", ""));
    }

    @Test
    void configuredRoleRequiresTheExactNormalizedEmailAndPhonePair() {
        when(otpHasher.hash(anyString())).thenReturn("test-hash");
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertRole("ADMIN@example.com", "+998901234567", "ADMIN");
        assertRole("someone@example.com", "+998901234567", "USER");
        assertRole("admin@example.com", "+998900000000", "USER");
        assertRole("SUPERADMIN@example.com", "+998907654321", "SUPER_ADMIN");
    }

    private void assertRole(String rawEmail, String rawPhone, String expectedRole) {
        clearInvocations(userRoleService, tokenService);
        String phone = AuthService.normalizePhone(rawPhone);
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        OtpChallenge challenge = OtpChallenge.create(phone, "test-hash", "test-hash",
                Instant.now().plusSeconds(300), null);
        when(challengeRepository.findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(phone))
                .thenReturn(Optional.of(challenge));
        when(userRepository.existsByEmailIgnoreCaseAndPhoneNot(email, phone)).thenReturn(false);
        when(userRepository.findByPhone(phone)).thenReturn(Optional.empty());

        service.verifyOtp(rawPhone, rawEmail, "123456");

        verify(userRoleService).syncAuthenticationRoles(any(UUID.class), eq(expectedRole));
        ArgumentCaptor<UserAccount> user = ArgumentCaptor.forClass(UserAccount.class);
        verify(tokenService).issue(user.capture());
        assertEquals(email, user.getValue().getEmail());
    }
}
