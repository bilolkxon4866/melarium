package uz.melarium.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import uz.melarium.api.dto.NotificationPreferencesResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationPreferencesRepository {
    private final JdbcTemplate jdbcTemplate;

    public NotificationPreferencesRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public NotificationPreferencesResponse getOrCreate(UUID userId) {
        jdbcTemplate.update("insert into user_notification_preferences(user_id) values (?) on conflict (user_id) do nothing", userId);
        return jdbcTemplate.queryForObject("select investment_updates, report_updates, marketing_updates, push_enabled, updated_at from user_notification_preferences where user_id = ?",
                (rs, row) -> map(rs), userId);
    }

    public NotificationPreferencesResponse update(UUID userId, boolean investment, boolean reports, boolean marketing, boolean push) {
        jdbcTemplate.update("""
                insert into user_notification_preferences(user_id, investment_updates, report_updates, marketing_updates, push_enabled, updated_at)
                values (?, ?, ?, ?, ?, now())
                on conflict (user_id) do update set investment_updates = excluded.investment_updates,
                    report_updates = excluded.report_updates, marketing_updates = excluded.marketing_updates,
                    push_enabled = excluded.push_enabled, updated_at = now()
                """, userId, investment, reports, marketing, push);
        return getOrCreate(userId);
    }

    public boolean allowsInApp(UUID userId, String kind) {
        String column = switch (kind) {
            case "INVESTMENT", "PAYMENT" -> "investment_updates";
            case "REPORT" -> "report_updates";
            case "MARKETING" -> "marketing_updates";
            default -> null;
        };
        if (column == null) return true;
        Boolean enabled = jdbcTemplate.query("select " + column + " from user_notification_preferences where user_id = ?",
                rs -> rs.next() ? rs.getBoolean(1) : null, userId);
        return enabled == null || enabled;
    }

    private NotificationPreferencesResponse map(ResultSet rs) throws SQLException {
        return new NotificationPreferencesResponse(rs.getBoolean("investment_updates"), rs.getBoolean("report_updates"),
                rs.getBoolean("marketing_updates"), rs.getBoolean("push_enabled"), rs.getObject("updated_at", java.time.OffsetDateTime.class).toInstant());
    }
}
