package uz.melarium.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final String ISSUER = "melarium";
    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtService(
            @Value("${app.security.jwt-secret-base64}") String encodedSecret,
            @Value("${app.security.access-token-ttl}") Duration accessTokenTtl
    ) {
        byte[] secret = Base64.getDecoder().decode(encodedSecret);
        if (secret.length < 32) throw new IllegalStateException("JWT signing key kamida 256 bit bo‘lishi kerak");
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTokenTtl = accessTokenTtl;
    }

    public String createAccessToken(UUID userId, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!ISSUER.equals(claims.getIssuer())) throw new IllegalArgumentException("JWT issuer noto‘g‘ri");
        UUID userId = UUID.fromString(claims.getSubject());
        Object rawRoles = claims.get("roles");
        List<String> roles = rawRoles instanceof List<?> list
                ? list.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                : List.of();
        return new AuthenticatedUser(userId, roles);
    }

    public long accessTokenTtlSeconds() { return accessTokenTtl.toSeconds(); }
}
