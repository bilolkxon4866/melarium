package uz.melarium.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "sensor_readings")
public class SensorReading {
    @Id private UUID id;
    @Column(name = "apiary_id", nullable = false) private UUID apiaryId;
    @Column(name = "colony_id") private UUID colonyId;
    @Column(nullable = false, length = 40) private String metric;
    @Column(name = "reading_value", nullable = false, precision = 12, scale = 4) private BigDecimal readingValue;
    @Column(nullable = false, length = 16) private String unit;
    @Column(nullable = false, length = 24) private String source;
    @Column(name = "device_id", length = 120) private String deviceId;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected SensorReading() { }

    public static SensorReading create(UUID apiaryId, UUID colonyId, String metric, BigDecimal value, String unit, String source, Instant recordedAt) {
        SensorReading reading = new SensorReading();
        reading.id = UUID.randomUUID();
        reading.apiaryId = apiaryId;
        reading.colonyId = colonyId;
        reading.metric = metric;
        reading.readingValue = value;
        reading.unit = unit;
        reading.source = source;
        reading.recordedAt = recordedAt;
        return reading;
    }

    @PrePersist
    void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getApiaryId() { return apiaryId; }
    public UUID getColonyId() { return colonyId; }
    public String getMetric() { return metric; }
    public BigDecimal getReadingValue() { return readingValue; }
    public String getUnit() { return unit; }
    public String getSource() { return source; }
    public Instant getRecordedAt() { return recordedAt; }
}
