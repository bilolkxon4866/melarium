package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import uz.melarium.domain.SensorReading;

public record SensorReadingResponse(UUID id, UUID apiaryId, UUID colonyId, String metric, BigDecimal value, String unit, String source, Instant recordedAt) {
    public static SensorReadingResponse from(SensorReading item) {
        return new SensorReadingResponse(item.getId(), item.getApiaryId(), item.getColonyId(), item.getMetric(), item.getReadingValue(), item.getUnit(), item.getSource(), item.getRecordedAt());
    }
}
