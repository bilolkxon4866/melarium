package uz.melarium.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import uz.melarium.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByReportedByOrderBySpentOnDesc(UUID reportedBy);
    List<Expense> findAllByOrderBySpentOnDesc();
    List<Expense> findByApiaryIdInOrderBySpentOnDesc(List<UUID> apiaryIds);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.apiaryId in :apiaryIds and e.spentOn between :from and :to")
    BigDecimal sumAmountByApiariesAndPeriod(@Param("apiaryIds") List<UUID> apiaryIds,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to);
}
