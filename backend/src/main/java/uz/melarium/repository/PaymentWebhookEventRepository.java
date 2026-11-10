package uz.melarium.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentWebhookEventRepository {
    private final JdbcTemplate jdbcTemplate;

    public PaymentWebhookEventRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public Optional<String> findPayloadHash(String provider, String eventId) {
        return jdbcTemplate.query("select payload_hash from payment_webhook_events where provider = ? and event_id = ?",
                rs -> rs.next() ? Optional.of(rs.getString(1)) : Optional.empty(), provider, eventId);
    }

    public void insert(String provider, String eventId, String payloadHash, UUID paymentId, String outcome) {
        jdbcTemplate.update("insert into payment_webhook_events(provider, event_id, payload_hash, payment_id, outcome) values (?, ?, ?, ?, ?)",
                provider, eventId, payloadHash, paymentId, outcome);
    }

    public void setOutcome(String provider, String eventId, String outcome) {
        jdbcTemplate.update("update payment_webhook_events set outcome = ? where provider = ? and event_id = ?", outcome, provider, eventId);
    }
}
