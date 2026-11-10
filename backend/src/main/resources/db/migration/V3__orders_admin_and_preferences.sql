ALTER TABLE marketplace_listings
    ADD COLUMN moderation_note TEXT NOT NULL DEFAULT '',
    ADD COLUMN reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN reviewed_at TIMESTAMPTZ;

ALTER TABLE investments
    ADD COLUMN reservation_expires_at TIMESTAMPTZ,
    ADD COLUMN idempotency_key VARCHAR(120);

ALTER TABLE investments DROP CONSTRAINT investments_status_check;
ALTER TABLE investments ADD CONSTRAINT investments_status_check
    CHECK (status IN ('PENDING_PAYMENT', 'ACTIVE', 'CANCELLED', 'REFUNDED', 'COMPLETED', 'PAYMENT_REVIEW'));

CREATE UNIQUE INDEX idx_investments_idempotency
    ON investments(investor_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_investments_expiring_reservations
    ON investments(status, reservation_expires_at)
    WHERE status = 'PENDING_PAYMENT';

CREATE UNIQUE INDEX idx_payments_provider_transaction_unique
    ON payments(provider, provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;

CREATE TABLE payment_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(24) NOT NULL,
    event_id VARCHAR(160) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    payment_id UUID REFERENCES payments(id) ON DELETE SET NULL,
    outcome VARCHAR(24) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT payment_webhook_events_provider_check CHECK (provider IN ('CLICK', 'PAYME', 'UZCARD', 'HUMO')),
    CONSTRAINT payment_webhook_events_outcome_check CHECK (outcome IN ('PROCESSING', 'SUCCESS', 'REJECTED')),
    CONSTRAINT payment_webhook_events_unique UNIQUE (provider, event_id)
);
CREATE INDEX idx_payment_webhook_events_received ON payment_webhook_events(received_at DESC);

CREATE TABLE user_notification_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    investment_updates BOOLEAN NOT NULL DEFAULT TRUE,
    report_updates BOOLEAN NOT NULL DEFAULT TRUE,
    marketing_updates BOOLEAN NOT NULL DEFAULT FALSE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
