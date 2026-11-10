package uz.melarium.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import uz.melarium.domain.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {
    Optional<OtpChallenge> findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(String phone);
    long countByPhoneAndCreatedAtAfter(String phone, Instant after);
    long countByRequestIpAndCreatedAtAfter(String requestIp, Instant after);
}
