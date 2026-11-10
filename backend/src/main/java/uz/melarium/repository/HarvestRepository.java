package uz.melarium.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import uz.melarium.domain.Harvest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HarvestRepository extends JpaRepository<Harvest, UUID> {
    List<Harvest> findByReportedByOrderByHarvestedOnDesc(UUID reportedBy);
    List<Harvest> findAllByOrderByHarvestedOnDesc();
    List<Harvest> findByApiaryIdInAndHarvestedOnBetweenOrderByHarvestedOnDesc(List<UUID> apiaryIds, LocalDate from, LocalDate to);
    List<Harvest> findByApiaryIdInAndStatusInOrderByHarvestedOnDesc(List<UUID> apiaryIds, List<String> statuses);
    long countByStatus(String status);

    @Query("select coalesce(sum(h.quantityKg), 0) from Harvest h where h.apiaryId in :apiaryIds and h.harvestedOn between :from and :to and h.status in ('SUBMITTED', 'APPROVED')")
    BigDecimal sumQuantityByApiariesAndPeriod(@Param("apiaryIds") List<UUID> apiaryIds,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);
}
