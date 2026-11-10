package uz.melarium.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uz.melarium.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByInvestmentIdAndIdempotencyKey(UUID investmentId, String idempotencyKey);
    List<Payment> findByInvestmentIdOrderByCreatedAtDesc(UUID investmentId);
    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(String status);

    @Query("select payment from Payment payment join Investment investment on investment.id = payment.investmentId where investment.investorId = :investorId order by payment.createdAt desc")
    Page<Payment> findByInvestorId(@Param("investorId") UUID investorId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.id = :id and payment.provider = :provider")
    Optional<Payment> findByIdAndProviderForUpdate(@Param("id") UUID id, @Param("provider") String provider);

    Optional<Payment> findByProviderAndProviderTransactionId(String provider, String providerTransactionId);
}
