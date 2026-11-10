package uz.melarium.repository;

import java.util.List;
import java.util.UUID;

import uz.melarium.domain.SensorReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorReadingRepository extends JpaRepository<SensorReading, UUID> {
    Page<SensorReading> findByApiaryIdOrderByRecordedAtDesc(UUID apiaryId, Pageable pageable);
}
