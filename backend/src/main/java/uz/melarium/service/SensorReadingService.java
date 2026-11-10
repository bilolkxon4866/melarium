package uz.melarium.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import uz.melarium.api.dto.SensorReadingRequest;
import uz.melarium.api.dto.SensorReadingResponse;
import uz.melarium.audit.AuditService;
import uz.melarium.repository.ColonyRepository;
import uz.melarium.repository.SensorReadingRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SensorReadingService {
    private final SensorReadingRepository sensorRepository;
    private final ColonyRepository colonyRepository;
    private final ApiaryAccessService apiaryAccessService;
    private final AuditService auditService;

    public SensorReadingService(SensorReadingRepository sensorRepository, ColonyRepository colonyRepository, ApiaryAccessService apiaryAccessService, AuditService auditService) {
        this.sensorRepository = sensorRepository;
        this.colonyRepository = colonyRepository;
        this.apiaryAccessService = apiaryAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<SensorReadingResponse> list(UUID apiaryId, int page, int size, AuthenticatedUser actor) {
        apiaryAccessService.requireAccess(apiaryId, actor);
        return sensorRepository.findByApiaryIdOrderByRecordedAtDesc(apiaryId, PageRequest.of(page, size)).map(SensorReadingResponse::from);
    }

    @Transactional
    public SensorReadingResponse create(SensorReadingRequest body, AuthenticatedUser actor) {
        apiaryAccessService.requireAccess(body.apiaryId(), actor);
        if (body.colonyId() != null && colonyRepository.findByIdAndApiaryId(body.colonyId(), body.apiaryId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ari oilasi ko‘rsatilgan asalarixonaga tegishli emas");
        }
        validateReading(body.metric(), body.value(), body.unit(), body.recordedAt());
        var saved = sensorRepository.save(uz.melarium.domain.SensorReading.create(body.apiaryId(), body.colonyId(), body.metric(), body.value(), body.unit(), body.source(), body.recordedAt()));
        auditService.record(actor.id(), "SENSOR_READING_ADDED", "SENSOR_READING", saved.getId(), "SUCCESS", java.util.Map.of("metric", body.metric(), "source", body.source()));
        return SensorReadingResponse.from(saved);
    }

    private void validateReading(String metric, BigDecimal value, String unit, Instant recordedAt) {
        if (recordedAt.isAfter(Instant.now().plus(Duration.ofMinutes(5)))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kelajakdagi o‘lchov sanasini yuborib bo‘lmaydi");
        }
        BigDecimal min;
        BigDecimal max;
        String expectedUnit;
        switch (metric) {
            case "TEMPERATURE" -> { min = BigDecimal.valueOf(-50); max = BigDecimal.valueOf(80); expectedUnit = "C"; }
            case "HUMIDITY" -> { min = BigDecimal.ZERO; max = BigDecimal.valueOf(100); expectedUnit = "%"; }
            case "WEIGHT" -> { min = BigDecimal.ZERO; max = BigDecimal.valueOf(1000); expectedUnit = "kg"; }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sensor metric qo‘llab-quvvatlanmaydi");
        }
        if (!expectedUnit.equals(unit) || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metric qiymati yoki o‘lchov birligi noto‘g‘ri");
        }
    }
}
