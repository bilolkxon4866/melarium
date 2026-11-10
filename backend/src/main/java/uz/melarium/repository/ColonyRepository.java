package uz.melarium.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uz.melarium.domain.Colony;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColonyRepository extends JpaRepository<Colony, UUID> {
    List<Colony> findByApiaryIdOrderByCreatedAtDesc(UUID apiaryId);
    long countByApiaryIdIn(List<UUID> apiaryIds);
    Optional<Colony> findByIdAndApiaryId(UUID id, UUID apiaryId);
    long countByApiaryId(UUID apiaryId);
}
