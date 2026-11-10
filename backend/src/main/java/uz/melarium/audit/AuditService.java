package uz.melarium.audit;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuditService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void record(UUID actorId, String action, String entityType, UUID entityId, String outcome, Object details) {
        try {
            String json = objectMapper.writeValueAsString(details == null ? java.util.Map.of() : details);
            jdbcTemplate.update(
                    "insert into audit_logs(actor_id, action, entity_type, entity_id, outcome, details) values (?, ?, ?, ?, ?, cast(? as jsonb))",
                    actorId, action, entityType, entityId, outcome, json
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Audit ma’lumoti JSON ga aylantirilmadi", exception);
        }
    }
}
