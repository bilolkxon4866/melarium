package uz.melarium.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uz.melarium.domain.Investment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvestmentRepository extends JpaRepository<Investment, UUID> {
    List<Investment> findByInvestorIdOrderByCreatedAtDesc(UUID investorId);
    Optional<Investment> findByIdAndInvestorId(UUID id, UUID investorId);
    Optional<Investment> findByInvestorIdAndIdempotencyKey(UUID investorId, String idempotencyKey);
    List<Investment> findByStatusAndReservationExpiresAtBeforeOrderByReservationExpiresAtAsc(String status, Instant before);
    Page<Investment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select investment from Investment investment where investment.id = :id")
    Optional<Investment> findByIdForUpdate(@Param("id") UUID id);

    @Query("select count(investment) from Investment investment where investment.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("select investment from Investment investment where investment.investorId = :investorId and investment.status = 'ACTIVE'")
    List<Investment> findActiveByInvestorId(@Param("investorId") UUID investorId);

    @Query("select distinct investment.investorId from Investment investment join MarketplaceListing listing on listing.id = investment.listingId where listing.apiaryId = :apiaryId and investment.status = 'ACTIVE'")
    List<UUID> findActiveInvestorIdsForApiary(@Param("apiaryId") UUID apiaryId);
}
