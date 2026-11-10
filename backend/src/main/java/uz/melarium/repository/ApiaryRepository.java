package uz.melarium.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uz.melarium.domain.Apiary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiaryRepository extends JpaRepository<Apiary, UUID> {
    List<Apiary> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    long countByOwnerId(UUID ownerId);
    Optional<Apiary> findByIdAndOwnerId(UUID id, UUID ownerId);
}
