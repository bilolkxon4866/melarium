CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(160),
    email VARCHAR(255),
    account_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    phone_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT users_status_check CHECK (account_status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE TABLE roles (
    id SMALLSERIAL PRIMARY KEY,
    code VARCHAR(24) NOT NULL UNIQUE,
    display_name VARCHAR(60) NOT NULL
);

INSERT INTO roles (code, display_name) VALUES
    ('INVESTOR', 'Investor'),
    ('BEEKEEPER', 'Asalarichi'),
    ('ADMIN', 'Administrator'),
    ('SUPER_ADMIN', 'Super administrator');

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id SMALLINT NOT NULL REFERENCES roles(id),
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE apiaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(160) NOT NULL,
    location VARCHAR(240) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT apiaries_status_check CHECK (status IN ('ACTIVE', 'SEASONAL', 'INACTIVE', 'PENDING'))
);
CREATE INDEX idx_apiaries_owner ON apiaries(owner_id);

CREATE TABLE colonies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    apiary_id UUID NOT NULL REFERENCES apiaries(id) ON DELETE CASCADE,
    code VARCHAR(40),
    colony_count INTEGER NOT NULL DEFAULT 1 CHECK (colony_count > 0),
    strength VARCHAR(24),
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    description TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT colonies_status_check CHECK (status IN ('ACTIVE', 'OBSERVATION', 'INACTIVE', 'LOST'))
);
CREATE INDEX idx_colonies_apiary ON colonies(apiary_id);

CREATE TABLE marketplace_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    beekeeper_id UUID NOT NULL REFERENCES users(id),
    apiary_id UUID NOT NULL REFERENCES apiaries(id),
    colony_id UUID REFERENCES colonies(id) ON DELETE SET NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(40) NOT NULL DEFAULT 'COLONY',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    location VARCHAR(240) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'UZS',
    unit_price NUMERIC(14, 2) NOT NULL CHECK (unit_price >= 0),
    units_total INTEGER NOT NULL CHECK (units_total > 0),
    units_available INTEGER NOT NULL CHECK (units_available >= 0 AND units_available <= units_total),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT marketplace_listings_status_check CHECK (status IN ('DRAFT', 'MODERATION', 'ACTIVE', 'RESERVED', 'SOLD', 'INACTIVE'))
);
CREATE INDEX idx_listings_status_created ON marketplace_listings(status, created_at DESC);
CREATE INDEX idx_listings_beekeeper ON marketplace_listings(beekeeper_id);
CREATE INDEX idx_listings_location ON marketplace_listings(location);

CREATE TABLE investments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id UUID NOT NULL REFERENCES users(id),
    listing_id UUID NOT NULL REFERENCES marketplace_listings(id),
    units_quantity INTEGER NOT NULL CHECK (units_quantity > 0),
    principal_amount NUMERIC(14, 2) NOT NULL CHECK (principal_amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'UZS',
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING_PAYMENT',
    terms_version VARCHAR(40),
    terms_accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT investments_status_check CHECK (status IN ('PENDING_PAYMENT', 'ACTIVE', 'CANCELLED', 'REFUNDED', 'COMPLETED'))
);
CREATE INDEX idx_investments_investor_created ON investments(investor_id, created_at DESC);
CREATE INDEX idx_investments_listing ON investments(listing_id);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investment_id UUID NOT NULL REFERENCES investments(id),
    provider VARCHAR(24) NOT NULL,
    provider_transaction_id VARCHAR(160),
    idempotency_key VARCHAR(160) UNIQUE,
    amount NUMERIC(14, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'UZS',
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    provider_payload JSONB,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT payments_provider_check CHECK (provider IN ('CLICK', 'PAYME', 'UZCARD', 'HUMO', 'MANUAL')),
    CONSTRAINT payments_status_check CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED'))
);
CREATE INDEX idx_payments_investment ON payments(investment_id);
CREATE INDEX idx_payments_provider_transaction ON payments(provider, provider_transaction_id);

CREATE TABLE harvests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    apiary_id UUID NOT NULL REFERENCES apiaries(id),
    colony_id UUID REFERENCES colonies(id) ON DELETE SET NULL,
    reported_by UUID NOT NULL REFERENCES users(id),
    quantity_kg NUMERIC(12, 3) NOT NULL CHECK (quantity_kg >= 0),
    harvested_on DATE NOT NULL,
    note TEXT NOT NULL DEFAULT '',
    status VARCHAR(24) NOT NULL DEFAULT 'SUBMITTED',
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT harvests_status_check CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'RETURNED'))
);
CREATE INDEX idx_harvests_apiary_date ON harvests(apiary_id, harvested_on DESC);
CREATE INDEX idx_harvests_reporter ON harvests(reported_by, created_at DESC);

CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    apiary_id UUID NOT NULL REFERENCES apiaries(id),
    colony_id UUID REFERENCES colonies(id) ON DELETE SET NULL,
    reported_by UUID NOT NULL REFERENCES users(id),
    category VARCHAR(60) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'UZS',
    spent_on DATE NOT NULL,
    note TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_expenses_apiary_date ON expenses(apiary_id, spent_on DESC);

CREATE TABLE sensor_readings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    apiary_id UUID NOT NULL REFERENCES apiaries(id) ON DELETE CASCADE,
    colony_id UUID REFERENCES colonies(id) ON DELETE SET NULL,
    metric VARCHAR(40) NOT NULL,
    reading_value NUMERIC(12, 4) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    source VARCHAR(24) NOT NULL DEFAULT 'MANUAL',
    device_id VARCHAR(120),
    recorded_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT sensor_readings_source_check CHECK (source IN ('MANUAL', 'SIMULATED', 'DEVICE'))
);
CREATE INDEX idx_sensor_readings_apiary_time ON sensor_readings(apiary_id, recorded_at DESC);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    body TEXT NOT NULL,
    kind VARCHAR(32) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, read_at, created_at DESC);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    outcome VARCHAR(24) NOT NULL DEFAULT 'SUCCESS',
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_actor_time ON audit_logs(actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
