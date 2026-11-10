package uz.melarium.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import uz.melarium.api.dto.TokenResponse;
import uz.melarium.domain.RefreshToken;
import uz.melarium.domain.UserAccount;
import uz.melarium.repository.RefreshTokenRepository;
import uz.melarium.repository.UserAccountRepository;
import uz.melarium.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TokenService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserAccountRepository userRepository;
    private final UserRoleService userRoleService;
    private final JwtService jwtService;
    private final Duration refreshTokenTtl;

    public TokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserAccountRepository userRepository,
            UserRoleService userRoleService,
            JwtService jwtService,
            @Value("${app.security.refresh-token-ttl}") Duration refreshTokenTtl
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.userRoleService = userRoleService;
        this.jwtService = jwtService;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public TokenResponse issue(UserAccount user) {
        List<String> roles = userRoleService.rolesFor(user.getId());
        return issuePair(user, roles).response();
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public TokenResponse rotate(String rawRefreshToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hashToken(rawRefreshToken))
                .orElseThrow(() -> unauthorized("Refresh token yaroqsiz yoki bekor qilingan"));
        Instant now = Instant.now();
        if (current.getRevokedAt() != null) {
            // Reuse of a rotated token is treated as potential theft: revoke all sessions for this user.
            refreshTokenRepository.revokeAllForUser(current.getUserId());
            throw unauthorized("Refresh token qayta ishlatilgan; sessiyalar bekor qilindi");
        }
        if (!current.getExpiresAt().isAfter(now)) {
            current.revoke(null);
            refreshTokenRepository.save(current);
            throw unauthorized("Refresh token muddati tugagan");
        }
        UserAccount user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> unauthorized("Foydalanuvchi topilmadi"));
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            current.revoke(null);
            refreshTokenRepository.save(current);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hisob faol emas");
        }
        Pair pair = issuePair(user, userRoleService.rolesFor(user.getId()));
        current.revoke(pair.refresh().getId());
        refreshTokenRepository.save(current);
        return pair.response();
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        refreshTokenRepository.findByTokenHashForUpdate(hashToken(rawRefreshToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> {
                    token.revoke(null);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public int revokeAllForUser(UUID userId) {
        return refreshTokenRepository.revokeAllForUser(userId);
    }

    private Pair issuePair(UserAccount user, List<String> roles) {
        String refreshRaw = generateRefreshToken();
        RefreshToken saved = refreshTokenRepository.save(
                RefreshToken.create(user.getId(), hashToken(refreshRaw), Instant.now().plus(refreshTokenTtl))
        );
        TokenResponse response = new TokenResponse(
                jwtService.createAccessToken(user.getId(), roles),
                refreshRaw,
                "Bearer",
                jwtService.accessTokenTtlSeconds(),
                new TokenResponse.UserView(user.getId(), user.getPhone(), user.getFullName(), roles)
        );
        return new Pair(response, saved);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Refresh token hash hisoblanmadi", exception);
        }
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private record Pair(TokenResponse response, RefreshToken refresh) { }
}
