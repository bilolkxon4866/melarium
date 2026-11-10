package uz.melarium.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import uz.melarium.api.dto.OtpRequestResponse;
import uz.melarium.api.dto.TokenResponse;
import uz.melarium.audit.AuditService;
import uz.melarium.domain.OtpChallenge;
import uz.melarium.domain.UserAccount;
import uz.melarium.repository.OtpChallengeRepository;
import uz.melarium.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_WINDOW = Duration.ofMinutes(10);

    private final OtpChallengeRepository challengeRepository;
    private final UserAccountRepository userRepository;
    private final OtpHasher otpHasher;
    private final SmsSender smsSender;
    private final UserRoleService userRoleService;
    private final TokenService tokenService;
    private final AuditService auditService;
    private final Environment environment;
    private final boolean exposeOtp;
    private final String bootstrapAdminPhone;

    public AuthService(
            OtpChallengeRepository challengeRepository,
            UserAccountRepository userRepository,
            OtpHasher otpHasher,
            SmsSender smsSender,
            UserRoleService userRoleService,
            TokenService tokenService,
            AuditService auditService,
            Environment environment,
            @Value("${app.security.expose-otp:false}") boolean exposeOtp,
            @Value("${app.security.bootstrap-admin-phone:}") String bootstrapAdminPhone
    ) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.otpHasher = otpHasher;
        this.smsSender = smsSender;
        this.userRoleService = userRoleService;
        this.tokenService = tokenService;
        this.auditService = auditService;
        this.environment = environment;
        this.exposeOtp = exposeOtp;
        this.bootstrapAdminPhone = bootstrapAdminPhone == null ? "" : bootstrapAdminPhone.trim();
    }

    @Transactional
    public OtpRequestResponse requestOtp(String rawPhone, String requestIp) {
        String phone = normalizePhone(rawPhone);
        Instant now = Instant.now();
        Instant windowStart = now.minus(RATE_WINDOW);
        if (challengeRepository.countByPhoneAndCreatedAtAfter(phone, windowStart) >= 3) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bu raqam uchun OTP so‘rov limiti tugadi; keyinroq urinib ko‘ring");
        }
        if (requestIp != null && !requestIp.isBlank() && challengeRepository.countByRequestIpAndCreatedAtAfter(requestIp, windowStart) >= 12) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "IP bo‘yicha OTP so‘rov limiti tugadi");
        }
        Optional<OtpChallenge> previous = challengeRepository.findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(phone);
        if (previous.isPresent() && previous.get().getCreatedAt().plusSeconds(30).isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Yangi kod so‘rashdan oldin 30 soniya kuting");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        OtpChallenge challenge = OtpChallenge.create(phone, otpHasher.hash(code), now.plus(OTP_TTL), requestIp);
        challengeRepository.save(challenge);
        smsSender.sendOtp(phone, code);
        auditService.record(null, "AUTH_OTP_REQUEST", "OTP_CHALLENGE", challengeId(challenge), "SUCCESS", Map.of("phoneSuffix", phone.substring(phone.length() - 4)));

        String debugCode = exposeOtp && environment.acceptsProfiles(Profiles.of("local")) ? code : null;
        return new OtpRequestResponse(phone, OTP_TTL.toSeconds(), debugCode == null ? "SMS" : "LOCAL_DEMO", debugCode);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public TokenResponse verifyOtp(String rawPhone, String code) {
        String phone = normalizePhone(rawPhone);
        Instant now = Instant.now();
        OtpChallenge challenge = challengeRepository.findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> unauthorized("Faol OTP so‘rovi topilmadi"));
        if (challenge.isExpired(now)) throw unauthorized("OTP kodi muddati tugagan; yangi kod so‘rang");
        if (challenge.isLocked()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OTP urinishlari limiti tugadi");

        String suppliedHash = otpHasher.hash(code);
        boolean matches = MessageDigest.isEqual(
                challenge.getCodeHash().getBytes(StandardCharsets.US_ASCII),
                suppliedHash.getBytes(StandardCharsets.US_ASCII)
        );
        if (!matches) {
            challenge.registerFailedAttempt();
            challengeRepository.save(challenge);
            auditService.record(null, "AUTH_OTP_VERIFY", "OTP_CHALLENGE", null, "FAILED", Map.of("reason", "invalid_code", "phoneSuffix", phone.substring(phone.length() - 4)));
            throw unauthorized("OTP kodi noto‘g‘ri");
        }

        challenge.markUsed();
        challengeRepository.save(challenge);
        UserAccount user = userRepository.findByPhone(phone).orElseGet(() -> userRepository.save(UserAccount.createVerified(phone)));
        if ("SUSPENDED".equals(user.getAccountStatus()) || "CLOSED".equals(user.getAccountStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hisob holati tizimga kirishga ruxsat bermaydi");
        }
        user.verifyPhone();
        user = userRepository.saveAndFlush(user);
        String bootstrapPhone = bootstrapAdminPhone.isBlank() ? "" : normalizePhone(bootstrapAdminPhone);
        String initialRole = !bootstrapPhone.isBlank() && bootstrapPhone.equals(phone) ? "SUPER_ADMIN" : "USER";
        userRoleService.assignIfAbsent(user.getId(), initialRole, null);
        auditService.record(user.getId(), "AUTH_OTP_VERIFY", "USER", user.getId(), "SUCCESS", Map.of("method", "OTP"));
        return tokenService.issue(user);
    }

    public TokenResponse refresh(String refreshToken) {
        return tokenService.rotate(refreshToken);
    }

    public void logout(String refreshToken) {
        tokenService.revoke(refreshToken);
    }

    public static String normalizePhone(String rawPhone) {
        if (rawPhone == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefon raqam kiritilishi shart");
        String phone = rawPhone.trim().replaceAll("[\\s()\\-]", "");
        if (phone.matches("^998[0-9]{9}$")) phone = "+" + phone;
        if (!phone.matches("^\\+998[0-9]{9}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefon raqam +998XXXXXXXXX formatida bo‘lishi kerak");
        }
        return phone;
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private java.util.UUID challengeId(OtpChallenge challenge) {
        return challenge.getId();
    }
}
