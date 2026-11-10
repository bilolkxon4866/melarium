package uz.melarium.audit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import uz.melarium.api.dto.AuditLogResponse;
import uz.melarium.api.dto.PageResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuditQueryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(UUID actorId, String action, int page, int size) {
        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> args = new ArrayList<>();
        if (actorId != null) { where.append(" and actor_id = ?"); args.add(actorId); }
        if (action != null && !action.isBlank()) { where.append(" and action = ?"); args.add(action.trim()); }
        String filter = where.toString();
        Long totalValue = jdbcTemplate.queryForObject("select count(*) from audit_logs" + filter, Long.class, args.toArray());
        long total = totalValue == null ? 0 : totalValue;
        int offset = Math.multiplyExact(page, size);
        String sql = "select id, actor_id, action, entity_type, entity_id, outcome, details::text as details, created_at from audit_logs" + filter +
                " order by created_at desc limit ? offset ?";
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add(offset);
        List<AuditLogResponse> rows = jdbcTemplate.query(sql, pageArgs.toArray(), this::map);
        int totalPages = (int) Math.ceil(total / (double) size);
        return new PageResponse<>(rows, page, size, total, totalPages, page == 0, totalPages == 0 || page >= totalPages - 1);
    }

    private AuditLogResponse map(ResultSet rs, int row) throws SQLException {
        Map<String, Object> details;
        try {
            String raw = rs.getString("details");
            details = raw == null ? Map.of() : objectMapper.readValue(raw, new TypeReference<>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("Audit details JSON o‘qilmadi", exception);
        }
        Timestamp timestamp = rs.getTimestamp("created_at");
        return new AuditLogResponse(rs.getObject("id", UUID.class), rs.getObject("actor_id", UUID.class),
                rs.getString("action"), rs.getString("entity_type"), rs.getObject("entity_id", UUID.class),
                rs.getString("outcome"), details, timestamp == null ? null : timestamp.toInstant());
    }
}
