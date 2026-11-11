package uz.melarium.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uz.melarium.domain.ListingStatus;
import uz.melarium.domain.MarketplaceListing;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, UUID> {

    @Query("""
            select listing from MarketplaceListing listing
            where listing.status = :status
              and (:query = '' or lower(listing.title) like lower(concat('%', :query, '%'))
                   or lower(listing.location) like lower(concat('%', :query, '%'))
                   or lower(listing.description) like lower(concat('%', :query, '%')))
            order by listing.createdAt desc
            """)
    Page<MarketplaceListing> searchByStatus(
            @Param("status") ListingStatus status,
            @Param("query") String query,
            Pageable pageable
    );

    Optional<MarketplaceListing> findByIdAndStatus(UUID id, ListingStatus status);
    List<MarketplaceListing> findByBeekeeperIdOrderByCreatedAtDesc(UUID beekeeperId);
    Optional<MarketplaceListing> findByIdAndBeekeeperId(UUID id, UUID beekeeperId);
    Page<MarketplaceListing> findByStatusOrderByCreatedAtDesc(ListingStatus status, Pageable pageable);
    long countByStatus(ListingStatus status);
    long countByBeekeeperIdAndStatus(UUID beekeeperId, ListingStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select listing from MarketplaceListing listing where listing.id = :id")
    Optional<MarketplaceListing> findByIdForUpdate(@Param("id") UUID id);
}
